package com.hotel.service;

import com.hotel.modelo.Reserva;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ReservaRepository reservaRepository;

    public ReporteService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public byte[] generarReservas() {
        List<Map<String, ?>> filas = new ArrayList<>();
        for (Reserva reserva : reservaRepository.findAll()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", String.valueOf(reserva.getIdReserva()));
            fila.put("cliente", reserva.getCliente().getNombre());
            fila.put("habitacion", reserva.getHabitacion().getNumero());
            fila.put("entrada", FECHA.format(reserva.getFechaEntrada()));
            fila.put("salida", FECHA.format(reserva.getFechaSalida()));
            fila.put("estado", reserva.getEstado());
            filas.add(fila);
        }
        return generarPdf("Listado de reservas", filas, List.of(
                new Columna("id", "Reserva", 55),
                new Columna("cliente", "Cliente", 190),
                new Columna("habitacion", "Habitación", 90),
                new Columna("entrada", "Entrada", 115),
                new Columna("salida", "Salida", 115),
                new Columna("estado", "Estado", 120)));
    }

    public byte[] generarProyeccionIngresos() {
        List<Map<String, ?>> filas = new ArrayList<>();
        for (Reserva reserva : reservaRepository.findAll()) {
            if ("CANCELADA".equalsIgnoreCase(reserva.getEstado())) {
                continue;
            }
            long noches = ChronoUnit.DAYS.between(reserva.getFechaEntrada(), reserva.getFechaSalida());
            BigDecimal precioNoche = BigDecimal.valueOf(reserva.getHabitacion()
                    .getTipoHabitacion().getPrecio());
            BigDecimal total = precioNoche.multiply(BigDecimal.valueOf(noches));

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", String.valueOf(reserva.getIdReserva()));
            fila.put("cliente", reserva.getCliente().getNombre());
            fila.put("habitacion", reserva.getHabitacion().getNumero());
            fila.put("noches", String.valueOf(noches));
            fila.put("precio", "S/ " + precioNoche.setScale(2).toPlainString());
            fila.put("proyeccion", "S/ " + total.setScale(2).toPlainString());
            filas.add(fila);
        }
        return generarPdf("Proyección de ingresos por reservas", filas, List.of(
                new Columna("id", "Reserva", 55),
                new Columna("cliente", "Cliente", 185),
                new Columna("habitacion", "Habitación", 90),
                new Columna("noches", "Noches", 75),
                new Columna("precio", "Precio por noche", 150),
                new Columna("proyeccion", "Total proyectado", 130)));
    }

    private byte[] generarPdf(String titulo, List<Map<String, ?>> filas, List<Columna> columnas) {
        try {
            JasperDesign design = crearDiseno(titulo, columnas);
            JasperReport reporte = JasperCompileManager.compileReport(design);
            JasperPrint resultado = JasperFillManager.fillReport(
                    reporte, new HashMap<>(), new JRMapCollectionDataSource(filas));
            return JasperExportManager.exportReportToPdf(resultado);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo generar el reporte PDF.", e);
        }
    }

    private JasperDesign crearDiseno(String titulo, List<Columna> columnas) throws JRException {
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
        titleBand.setHeight(48);
        JRDesignStaticText titleText = new JRDesignStaticText();
        titleText.setX(0);
        titleText.setY(8);
        titleText.setWidth(782);
        titleText.setHeight(30);
        titleText.setText(titulo);
        titleText.setFontSize(18f);
        titleText.setBold(true);
        titleBand.addElement(titleText);
        design.setTitle(titleBand);

        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(26);
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(24);
        int x = 0;
        for (Columna columna : columnas) {
            JRDesignField field = new JRDesignField();
            field.setName(columna.campo());
            field.setValueClass(String.class);
            design.addField(field);

            JRDesignStaticText header = new JRDesignStaticText();
            header.setX(x);
            header.setY(0);
            header.setWidth(columna.ancho());
            header.setHeight(24);
            header.setText(columna.etiqueta());
            header.setBold(true);
            header.setFontSize(10f);
            headerBand.addElement(header);

            JRDesignTextField value = new JRDesignTextField();
            value.setX(x);
            value.setY(0);
            value.setWidth(columna.ancho());
            value.setHeight(22);
            value.setFontSize(9f);
            JRDesignExpression expression = new JRDesignExpression();
            expression.setText("$F{" + columna.campo() + "}");
            value.setExpression(expression);
            detailBand.addElement(value);
            x += columna.ancho();
        }
        design.setColumnHeader(headerBand);
        design.getDetailSection().addBand(detailBand);
        return design;
    }

    private record Columna(String campo, String etiqueta, int ancho) { }
}
