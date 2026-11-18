package vn.biz04.service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

final class ContractSignature {
    private static final int WIDTH = 420;
    private static final int HEIGHT = 140;
    private static final int MAX_POINTS = 900;
    private static final Pattern POINT = Pattern.compile("([0-9]{1,3}),([0-9]{1,3})");
    private static final String INVALID = "Chữ ký không hợp lệ. Hãy xóa và ký lại trong ô chữ ký.";

    private ContractSignature() {}

    static byte[] png(String strokes) {
        Checks.require(strokes != null && !strokes.isBlank(), "Hãy ký tên trong ô chữ ký trước khi xác nhận.");
        Checks.require(strokes.length() <= MAX_POINTS * 8, INVALID);
        Path2D path = new Path2D.Double();
        int points = 0;
        double ink = 0;
        for (String stroke : strokes.split(";", -1)) {
            int previousX = -1;
            int previousY = -1;
            for (String point : stroke.split(" ", -1)) {
                var match = POINT.matcher(point);
                Checks.require(++points <= MAX_POINTS && match.matches(), INVALID);
                int x = Integer.parseInt(match.group(1));
                int y = Integer.parseInt(match.group(2));
                Checks.require(x < WIDTH && y < HEIGHT, INVALID);
                if (previousX < 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                    ink += Math.hypot(x - previousX, y - previousY);
                }
                previousX = x;
                previousY = y;
            }
        }
        Checks.require(ink >= 12, "Hãy vẽ chữ ký đủ nét trong ô chữ ký.");
        var image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(20, 38, 77));
            graphics.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(path);
        } finally {
            graphics.dispose();
        }
        try {
            var bytes = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", bytes)) throw new BusinessException("Không thể lưu chữ ký. Hãy thử lại.");
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Không thể lưu chữ ký. Hãy thử lại.");
        }
    }
}
