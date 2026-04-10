package com.project.customSwing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

public class ShadowPanel extends JPanel {

	public ShadowPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int shadowSize = 6;

        // shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRoundRect(shadowSize, shadowSize,
                getWidth() - shadowSize,
                getHeight() - shadowSize,
                20, 20);

        // card
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0,
                getWidth() - shadowSize,
                getHeight() - shadowSize,
                20, 20);

        super.paintComponent(g);
    }

}