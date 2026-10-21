package com.hotel.service;

import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.repository.HabitacionRepository;
import com.hotel.repository.ReservaRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reportes PDF con JasperReports. */
@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;

    public ReporteService(ReservaRepository reservaRepository, HabitacionRepository habitacionRepository) {
        this.reservaRepository = reservaRepository;
        this.habitacionRepository = habitacionRepository;
    }

    public byte[] generarReservas() {
        return generarReservas(null, null, null);
    }

    /** Reporte 1: reservas registradas, filtradas por período de entrada y estado. */
    public byte[] generarReservas(LocalDate desde, LocalDate hasta, String estado) {
        String estadoFiltro = (estado == null || estado.isBlank()) ? null : estado;
        List<Reserva> reservas = reservaRepository.buscar(desde, hasta, estadoFiltro, null, null);
        List<Map<String, ?>> filas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Reserva reserva : reservas) {
            BigDecimal importe = dinero(reserva.getTotal());
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", String.valueOf(reserva.getIdReserva()));
            fila.put("cliente", reserva.getCliente().getNombre());
            fila.put("documento", reserva.getCliente().getDocumento());
            fila.put("habitacion", reserva.getHabitacion().getNumero());
            fila.put("entrada", FECHA.format(reserva.getFechaEntrada()));
            fila.put("salida", FECHA.format(reserva.getFechaSalida()));
            fila.put("noches", String.valueOf(reserva.getNoches()));
            fila.put("estado", reserva.getEstado());
            fila.put("importe", "S/ " + importe.toPlainString());
            filas.add(fila);
            if (!ReservaService.CANCELADA.equals(reserva.getEstado())) {
                total = total.add(importe);
            }
        }
        String periodo = "Período de entrada: " + (desde == null ? "inicio" : FECHA.format(desde))
                + " al " + (hasta == null ? "hoy en adelante" : FECHA.format(hasta))
                + (estadoFiltro == null ? " · Todos los estados" : " · Estado: " + estadoFiltro);
        String resumen = "Total de reservas: " + filas.size()
                + "    ·    Importe estimado (sin canceladas): S/ " + total.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return generarPdf("Reporte de reservas", periodo, resumen, filas, List.of(
                new Columna("id", "N°", 40),
                new Columna("cliente", "Cliente", 170),
                new Columna("documento", "Documento", 85),
                new Columna("habitacion", "Hab.", 50),
                new Columna("entrada", "Entrada", 80),
                new Columna("salida", "Salida", 80),
                new Columna("noches", "Noches", 55),
                new Columna("estado", "Estado", 105),
                new Columna("importe", "Importe", 117)));
    }

    /** Reporte 2: habitaciones con su tipo, tarifa, estado actual y uso. */
    public byte[] generarHabitaciones() {
        List<Map<String, ?>> filas = new ArrayList<>();
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (Habitacion habitacion : habitacionRepository.findAllByOrderByNumeroAsc()) {
            long reservas = reservaRepository.countByHabitacionIdHabitacionAndEstadoNot(
                    habitacion.getIdHabitacion(), ReservaService.CANCELADA);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("numero", habitacion.getNumero());
            fila.put("piso", String.valueOf(habitacion.getPiso()));
            fila.put("tipo", habitacion.getTipoHabitacion() == null ? "-" : habitacion.getTipoHabitacion().getNombre());
            fila.put("precio", habitacion.getTipoHabitacion() == null ? "-"
                    : "S/ " + dinero(habitacion.getTipoHabitacion().getPrecio()).toPlainString());
            fila.put("estado", habitacion.getEstado());
            fila.put("reservas", String.valueOf(reservas));
            filas.add(fila);
            porEstado.merge(habitacion.getEstado(), 1, Integer::sum);
        }
        StringBuilder resumen = new StringBuilder("Total de habitaciones: " + filas.size());
        porEstado.forEach((estado, cantidad) -> resumen.append("    ·    ").append(estado).append(": ").append(cantidad));
        return generarPdf("Reporte de habitaciones", "Estado actual de las habitaciones del hotel",
                resumen.toString(), filas, List.of(
                new Columna("numero", "Habitación", 110),
                new Columna("piso", "Piso", 80),
                new Columna("tipo", "Tipo", 170),
                new Columna("precio", "Tarifa por noche", 150),
                new Columna("estado", "Estado actual", 150),
                new Columna("reservas", "N° reservas", 122)));
    }

    /** Reporte adicional: proyección de ingresos por reservas no canceladas. */
    public byte[] generarProyeccionIngresos() {
        List<Map<String, ?>> filas = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (Reserva reserva : reservaRepository.findAllByOrderByFechaEntradaDesc()) {
            if (ReservaService.CANCELADA.equalsIgnoreCase(reserva.getEstado())) {
                continue;
            }
            BigDecimal precioNoche = dinero(reserva.getHabitacion().getTipoHabitacion().getPrecio());
            BigDecimal total = dinero(reserva.getTotal());
            suma = suma.add(total);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", String.valueOf(reserva.getIdReserva()));
            fila.put("cliente", reserva.getCliente().getNombre());
            fila.put("habitacion", reserva.getHabitacion().getNumero());
            fila.put("noches", String.valueOf(reserva.getNoches()));
            fila.put("precio", "S/ " + precioNoche.toPlainString());
            fila.put("proyeccion", "S/ " + total.toPlainString());
            filas.add(fila);
        }
        return generarPdf("Proyección de ingresos por reservas",
                "Calculada con la tarifa por noche; no representa pagos registrados",
                "Total proyectado: S/ " + suma.setScale(2, RoundingMode.HALF_UP).toPlainString(), filas, List.of(
                new Columna("id", "Reserva", 55),
                new Columna("cliente", "Cliente", 185),
                new Columna("habitacion", "Habitación", 90),
                new Columna("noches", "Noches", 75),
                new Columna("precio", "Precio por noche", 150),
                new Columna("proyeccion", "Total proyectado", 130)));
    }

    private BigDecimal dinero(Double valor) {
        return BigDecimal.valueOf(valor == null ? 0 : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private byte[] generarPdf(String titulo, String subtitulo, String resumen,
                              List<Map<String, ?>> filas, List<Columna> columnas) {
        try {
            JasperDesign design = crearDiseno(titulo, subtitulo, resumen, columnas);
            JasperReport reporte = JasperCompileManager.compileReport(design);
            JasperPrint resultado = JasperFillManager.fillReport(
                    reporte, new HashMap<>(), new JRMapCollectionDataSource(filas));
            return JasperExportManager.exportReportToPdf(resultado);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el reporte PDF.", e);
        }
    }

    private JasperDesign crearDiseno(String titulo, String subtitulo, String resumen, List<Columna> columnas)
            throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("reporteHotel");
        design.setPageWidth(842);
        design.setPageHeight(595);
        design.setOrientation(net.sf.jasperreports.engine.type.OrientationEnum.LANDSCAPE);
        design.setLeftMargin(30);
        design.setRightMargin(30);
        design.setTopMargin(25);
        design.setBottomMargin(25);
        design.setColumnWidth(782);

        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(78);
        titleBand.addElement(texto("Hotel Los Andes", 0, 0, 782, 16, 10f, false));
        titleBand.addElement(texto(titulo, 0, 16, 782, 28, 18f, true));
        titleBand.addElement(texto(subtitulo, 0, 44, 520, 16, 9f, false));
        titleBand.addElement(texto("Generado: " + LocalDateTime.now().format(FECHA_HORA), 520, 44, 262, 16, 9f, false));
        design.setTitle(titleBand);

        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(26);
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(22);
        int x = 0;
        for (Columna columna : columnas) {
            JRDesignField field = new JRDesignField();
            field.setName(columna.campo());
            field.setValueClass(String.class);
            design.addField(field);

            headerBand.addElement(texto(columna.etiqueta(), x, 0, columna.ancho(), 24, 10f, true));

            JRDesignTextField value = new JRDesignTextField();
            value.setX(x);
            value.setY(0);
            value.setWidth(columna.ancho());
            value.setHeight(20);
            value.setFontSize(9f);
            JRDesignExpression expression = new JRDesignExpression();
            expression.setText("$F{" + columna.campo() + "}");
            value.setExpression(expression);
            detailBand.addElement(value);
            x += columna.ancho();
        }
        design.setColumnHeader(headerBand);
        design.getDetailSection().addBand(detailBand);

        JRDesignBand summaryBand = new JRDesignBand();
        summaryBand.setHeight(34);
        summaryBand.addElement(texto(resumen, 0, 12, 782, 18, 10f, true));
        design.setSummary(summaryBand);
        return design;
    }

    private JRDesignStaticText texto(String contenido, int x, int y, int ancho, int alto, float fuente, boolean negrita) {
        JRDesignStaticText texto = new JRDesignStaticText();
        texto.setX(x);
        texto.setY(y);
        texto.setWidth(ancho);
        texto.setHeight(alto);
        texto.setText(contenido);
        texto.setFontSize(fuente);
        texto.setBold(negrita);
        return texto;
    }

    private record Columna(String campo, String etiqueta, int ancho) { }
}
