package com.project.customSwing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
 
public class TableStyleUtil {
 
    private static final Color BLUE   = new Color(0, 150, 199);
    private static final Color WHITE  = Color.WHITE;
    private static final Color STRIPE = new Color(245, 248, 252);
 
    public static void applyStyle(JTable table) {
        int ROW_H = 36;
        table.setRowHeight(ROW_H);
        table.getTableHeader().setPreferredSize(new Dimension(0, ROW_H));
        table.getTableHeader().setBackground(BLUE);
        table.getTableHeader().setForeground(WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
 
        table.setBackground(WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setGridColor(new Color(230, 235, 245));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(200, 230, 255));
        table.setFillsViewportHeight(true);
 
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setOpaque(true);
                setBackground(sel ? new Color(200, 230, 255)
                                  : row % 2 == 0 ? WHITE : STRIPE);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        });
    }
 
    public static JScrollPane modernScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        sp.getViewport().setBackground(WHITE);
        styleBar(sp.getVerticalScrollBar());
        styleBar(sp.getHorizontalScrollBar());
        return sp;
    }
 
    private static void styleBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0, 150, 199, 120);
                trackColor = new Color(240, 244, 250);
            }
            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }
            private JButton zero() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setBorderPainted(false);
                return b;
            }
        });
    }
}