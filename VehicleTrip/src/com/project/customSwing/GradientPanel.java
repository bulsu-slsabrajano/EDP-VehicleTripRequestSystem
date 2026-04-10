package com.project.customSwing;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class GradientPanel extends JPanel {

	@Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        Color topColor = Color.decode("#0096C7");
        Color bottomColor = Color.decode("#0F2573");
        
        GradientPaint gp = new GradientPaint(
                0, 0, topColor, 
                0, getHeight(), bottomColor);
        
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
