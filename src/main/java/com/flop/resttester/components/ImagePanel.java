package com.flop.resttester.components;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImagePanel extends JPanel {

    private BufferedImage image;

    public void setImage(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try {
            this.image = ImageIO.read(bis);
        } catch (IOException e) {
            // Do nothing
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (this.image != null) {
            int width = Math.min(this.getWidth(), this.image.getWidth());
            int height = Math.min(this.getHeight(), this.image.getHeight());

            double imgWidth = this.image.getWidth();
            double imgHeight = this.image.getHeight();
            double scaleX = (imgWidth - (imgWidth - width)) / imgWidth;
            double scaleY = (imgHeight - (imgHeight - height)) / imgHeight;

            if (scaleX == 0) {
                scaleX = 1;
            }

            if (scaleY == 0) {
                scaleY = 1;
            }

            if (scaleX < scaleY) {
                height = (int) (imgHeight * scaleX);
            } else {
                width = (int) (imgWidth * scaleY);
            }

            int x = (this.getWidth() - width) / 2;
            int y = (this.getHeight() - height) / 2;

            g.drawImage(this.image, x, y, width, height, this);
        }
    }

    @Override
    public void setBackground(Color color) {
        // workaround to keep IntelliJ new UI from overriding the background.
    }

    public void setCustomBackground(Color color) {
        super.setBackground(color);
    }
}