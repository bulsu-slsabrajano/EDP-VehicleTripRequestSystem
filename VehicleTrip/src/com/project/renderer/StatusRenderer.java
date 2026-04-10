package com.project.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class StatusRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (value != null) {
            String status = value.toString();

            switch (status.toUpperCase()) {
                case "AVAILABLE", "COMPLETED" -> c.setForeground(new Color(0, 153, 0));
                case "UNAVAILABLE", "CANCELLED" -> c.setForeground(Color.RED);
                case "PENDING" -> c.setForeground(new Color(255, 140, 0));
                case "APPROVED"  -> c.setForeground(new Color(0, 150, 199));
                default -> c.setForeground(Color.GRAY);
            }
        }

        return c;
    }
}
