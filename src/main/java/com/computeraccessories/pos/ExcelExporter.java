package com.computeraccessories.pos;

import javax.swing.*;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ExcelExporter {
    private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";

    private ExcelExporter() {
    }

    static void export(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("techhub-computer-accessories-pos.xlsx"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new java.io.File(file.getParentFile(), file.getName() + ".xlsx");
        }

        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("Sales", "SELECT s.id, u.display_name cashier, s.subtotal, s.tax_rate, s.tax_amount, "
                + "s.total, s.payment_method, s.payment_provider, s.paid_at "
                + "FROM sales s JOIN users u ON u.id=s.cashier_id ORDER BY s.id");
        queries.put("Sale items", "SELECT si.id, si.sale_id, p.item_name, si.quantity, si.unit_price "
                + "FROM sale_items si JOIN products p ON p.id=si.product_id ORDER BY si.id");
        queries.put("Inventory", "SELECT id, barcode, item_name, photo, quantity, size, price FROM products ORDER BY id");
        queries.put("Purchases", "SELECT p.id, pr.item_name, s.name supplier, p.source_type, p.quantity, "
                + "p.unit_cost, p.purchased_at FROM purchases p JOIN products pr ON pr.id=p.product_id "
                + "JOIN suppliers s ON s.id=p.supplier_id ORDER BY p.id");
        queries.put("Suppliers", "SELECT id, name, phone, address, created_at FROM suppliers ORDER BY id");
        queries.put("VIP list", "SELECT id, name, phone, points, created_at FROM vip_members ORDER BY id");
        queries.put("Cashiers", "SELECT id, username, display_name, is_active, created_at "
                + "FROM users WHERE role='CASHIER' ORDER BY id");
        queries.put("Settings", "SELECT setting_key, setting_value FROM app_settings ORDER BY setting_key");

        try (Connection connection = Db.connect();
             ZipOutputStream workbook = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            writeTextEntry(workbook, "[Content_Types].xml", contentTypes(queries.size()));
            writeTextEntry(workbook, "_rels/.rels", packageRelationships());
            writeTextEntry(workbook, "xl/workbook.xml", workbookXml(queries));
            writeTextEntry(workbook, "xl/_rels/workbook.xml.rels", workbookRelationships(queries.size()));
            writeTextEntry(workbook, "xl/styles.xml", stylesXml());

            int sheetNumber = 1;
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                try (PreparedStatement query = connection.prepareStatement(entry.getValue());
                     ResultSet result = query.executeQuery()) {
                    writeSheet(workbook, sheetNumber++, result);
                }
            }
            workbook.finish();
            JOptionPane.showMessageDialog(parent, "Excel export saved:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Excel export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void writeSheet(ZipOutputStream workbook, int sheetNumber, ResultSet result) throws Exception {
        workbook.putNextEntry(new ZipEntry("xl/worksheets/sheet" + sheetNumber + ".xml"));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(workbook, StandardCharsets.UTF_8));
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.write("<worksheet xmlns=\"" + MAIN_NS + "\"><sheetData>");
        ResultSetMetaData metadata = result.getMetaData();
        int columns = metadata.getColumnCount();

        writer.write("<row r=\"1\">");
        for (int column = 1; column <= columns; column++) {
            writeStringCell(writer, column, 1, metadata.getColumnLabel(column), true);
        }
        writer.write("</row>");

        int rowNumber = 2;
        while (result.next()) {
            writer.write("<row r=\"" + rowNumber + "\">");
            for (int column = 1; column <= columns; column++) {
                Object value = result.getObject(column);
                if (value != null) writeCell(writer, column, rowNumber, value);
            }
            writer.write("</row>");
            rowNumber++;
        }
        writer.write("</sheetData></worksheet>");
        writer.flush();
        workbook.closeEntry();
    }

    private static void writeCell(BufferedWriter writer, int column, int row, Object value) throws Exception {
        String reference = columnName(column) + row;
        if (value instanceof Number number) {
            writer.write("<c r=\"" + reference + "\"><v>" + xml(number.toString()) + "</v></c>");
            return;
        }
        if (value instanceof Timestamp timestamp) value = timestamp.toLocalDateTime();
        if (value instanceof LocalDateTime dateTime) value = dateTime.toString();
        writeStringCell(writer, column, row, String.valueOf(value), false);
    }

    private static void writeStringCell(BufferedWriter writer, int column, int row,
                                       String value, boolean header) throws Exception {
        String reference = columnName(column) + row;
        writer.write("<c r=\"" + reference + "\" t=\"inlineStr\"");
        if (header) writer.write(" s=\"1\"");
        writer.write("><is><t xml:space=\"preserve\">" + xml(value) + "</t></is></c>");
    }

    private static String columnName(int column) {
        StringBuilder name = new StringBuilder();
        while (column > 0) {
            column--;
            name.insert(0, (char) ('A' + column % 26));
            column /= 26;
        }
        return name.toString();
    }

    private static String xml(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> {
                    if (ch == '\t' || ch == '\n' || ch == '\r' || ch >= 0x20) escaped.append(ch);
                }
            }
        }
        return escaped.toString();
    }

    private static void writeTextEntry(ZipOutputStream workbook, String path, String text) throws Exception {
        workbook.putNextEntry(new ZipEntry(path));
        workbook.write(text.getBytes(StandardCharsets.UTF_8));
        workbook.closeEntry();
    }

    private static String contentTypes(int sheetCount) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= sheetCount; i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private static String packageRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"" + PACKAGE_REL_NS + "\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbookXml(Map<String, String> sheets) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<workbook xmlns=\"").append(MAIN_NS).append("\" xmlns:r=\"").append(REL_NS)
                .append("\"><sheets>");
        int index = 1;
        for (String name : sheets.keySet()) {
            xml.append("<sheet name=\"").append(xml(name)).append("\" sheetId=\"").append(index)
                    .append("\" r:id=\"rId").append(index).append("\"/>");
            index++;
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private static String workbookRelationships(int sheetCount) {
        List<String> relationships = new ArrayList<>();
        for (int i = 1; i <= sheetCount; i++) {
            relationships.add("<Relationship Id=\"rId" + i + "\" Type=\""
                    + REL_NS + "/worksheet\" Target=\"worksheets/sheet" + i + ".xml\"/>");
        }
        relationships.add("<Relationship Id=\"rId" + (sheetCount + 1) + "\" Type=\""
                + REL_NS + "/styles\" Target=\"styles.xml\"/>");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"" + PACKAGE_REL_NS + "\">"
                + String.join("", relationships) + "</Relationships>";
    }

    private static String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"" + MAIN_NS + "\">"
                + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
                + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }
}
