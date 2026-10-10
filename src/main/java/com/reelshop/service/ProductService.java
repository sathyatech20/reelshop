package com.reelshop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.reelshop.entity.Product;
import com.reelshop.entity.Seller;
import com.reelshop.exception.ReelShopException;
import com.reelshop.repository.ProductRepository;
import com.reelshop.repository.SellerRepository;
import com.reelshop.util.ShortCodeGenerator;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository  productRepository;
    private final SellerRepository   sellerRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CloudinaryService  cloudinaryService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ── List products ─────────────────────────────────────
    public List<ProductDto> listProducts(Long sellerId) {
        return productRepository
            .findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, Product.ProductStatus.ACTIVE)
            .stream().map(this::toDto).toList();
    }

    // ── Create product ────────────────────────────────────
    @Transactional
    public ProductDto createProduct(Long sellerId, CreateRequest req) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> ReelShopException.notFound("Seller"));

        // Generate unique short code
        String shortCode;
        do {
            shortCode = shortCodeGenerator.generate();
        } while (productRepository.findByShortCode(shortCode).isPresent());

        Product product = Product.builder()
                .seller(seller)
                .name(req.name)
                .description(req.description)
                .price(new BigDecimal(req.price))
                .shortCode(shortCode)
                .stockStatus(Product.StockStatus.valueOf(
                    req.stockStatus != null ? req.stockStatus : "AVAILABLE"))
                .build();

        Product saved = productRepository.save(product);

        // Upload images
        if (req.images != null) {
            int order = 0;
            for (MultipartFile img : req.images) {
                if (img != null && !img.isEmpty()) {
                    try {
                        CloudinaryService.UploadResult result =
                            cloudinaryService.upload(img, "products");
                        saved.getImages().add(new Product.ProductImage(
                            result.url(), result.publicId(), order++));
                    } catch (Exception e) {
                        log.warn("Image upload failed: {}", e.getMessage());
                    }
                }
            }
            productRepository.save(saved);
        }

        return toDto(saved);
    }

    // ── Update product ────────────────────────────────────
    @Transactional
    public ProductDto updateProduct(Long sellerId, Long productId, UpdateRequest req) {
        Product product = getProductForSeller(sellerId, productId);

        if (req.name        != null) product.setName(req.name);
        if (req.description != null) product.setDescription(req.description);
        if (req.price       != null) product.setPrice(new BigDecimal(req.price));
        if (req.stockStatus != null)
            product.setStockStatus(Product.StockStatus.valueOf(req.stockStatus));

        return toDto(productRepository.save(product));
    }

    // ── Toggle stock ──────────────────────────────────────
    @Transactional
    public ProductDto toggleStock(Long sellerId, Long productId) {
        Product product = getProductForSeller(sellerId, productId);
        product.setStockStatus(
            product.getStockStatus() == Product.StockStatus.AVAILABLE
                ? Product.StockStatus.OUT_OF_STOCK
                : Product.StockStatus.AVAILABLE
        );
        return toDto(productRepository.save(product));
    }

    // ── Soft delete ───────────────────────────────────────
    @Transactional
    public void deleteProduct(Long sellerId, Long productId) {
        Product product = getProductForSeller(sellerId, productId);
        product.setStatus(Product.ProductStatus.DELETED);
        productRepository.save(product);
    }

    // ── Generate QR ───────────────────────────────────────
    public byte[] generateQrPng(Long sellerId, Long productId) throws Exception {
        Product product = getProductForSeller(sellerId, productId);
        String  url     = baseUrl.replace(":8080", "") + "/p/" + product.getShortCode();

        // Use frontend URL for QR — buyers scan this
        String qrUrl = "http://localhost:5173/p/" + product.getShortCode();

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION,
                  com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);

        QRCodeWriter writer    = new QRCodeWriter();
        BitMatrix    bitMatrix = writer.encode(qrUrl, BarcodeFormat.QR_CODE, 512, 512, hints);

        // Convert to image and add logo overlay
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        BufferedImage result  = addLogoOverlay(qrImage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(result, "PNG", baos);
        return baos.toByteArray();
    }

    // ── Add logo overlay to QR ────────────────────────────
    private BufferedImage addLogoOverlay(BufferedImage qr) {
        int size       = qr.getWidth();
        int logoSize   = size / 5;
        int logoX      = (size - logoSize) / 2;
        int logoY      = (size - logoSize) / 2;

        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D    g2d    = result.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(qr, 0, 0, null);

        // White circle background for logo
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(logoX - 6, logoY - 6, logoSize + 12, logoSize + 12, 16, 16);

        // Accent color square logo
        GradientPaint grad = new GradientPaint(
            logoX, logoY, new Color(0xFF4D6D),
            logoX + logoSize, logoY + logoSize, new Color(0xFF6B35)
        );
        g2d.setPaint(grad);
        g2d.fillRoundRect(logoX, logoY, logoSize, logoSize, 12, 12);

        // ◈ symbol
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, logoSize / 2));
        FontMetrics fm  = g2d.getFontMetrics();
        String      sym = "R";
        int         tx  = logoX + (logoSize - fm.stringWidth(sym)) / 2;
        int         ty  = logoY + (logoSize - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(sym, tx, ty);

        g2d.dispose();
        return result;
    }

    // ── Public product lookup (buyer) ─────────────────────
    public ProductDto getByShortCode(String shortCode) {
        return productRepository.findByShortCode(shortCode)
            .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
            .map(this::toDto)
            .orElseThrow(() -> ReelShopException.notFound("Product"));
    }

    // ── Increment scan count ──────────────────────────────
    @Transactional
    public void trackScan(String shortCode) {
        productRepository.findByShortCode(shortCode).ifPresent(p -> {
            productRepository.incrementQrScanCount(p.getId());
            sellerRepository.incrementQrScans(p.getSeller().getId());
        });
    }

    @Transactional
    public void trackWhatsapp(Long productId) {
        productRepository.incrementWhatsappClickCount(productId);
        productRepository.findById(productId).ifPresent(p ->
            sellerRepository.incrementWhatsappClicks(p.getSeller().getId()));
    }

    // ── Helpers ───────────────────────────────────────────
    private Product getProductForSeller(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ReelShopException.notFound("Product"));
        if (!product.getSeller().getId().equals(sellerId)) {
            throw ReelShopException.forbidden();
        }
        return product;
    }

    public ProductDto toDto(Product p) {
        var images = p.getImages().stream()
            .map(i -> new ImageDto(i.getImageUrl(), i.getDisplayOrder()))
            .toList();

        SellerInfo seller = new SellerInfo(
            p.getSeller().getId(),
            p.getSeller().getShopName(),
            p.getSeller().getShopSlug(),
            p.getSeller().getProfileImageUrl(),
            p.getSeller().getWhatsappNumber(),
            p.getSeller().getUpiId(),
            p.getSeller().getCategory() != null ? p.getSeller().getCategory().name() : null
        );

        return new ProductDto(
            p.getId(), p.getName(), p.getDescription(),
            p.getPrice().toPlainString(), p.getShortCode(),
            p.getStockStatus().name(), p.getStatus().name(),
            p.getQrScanCount(), p.getWhatsappClickCount(),
            images, seller
        );
    }

    // ── DTOs ──────────────────────────────────────────────
    public record ProductDto(
        Long   id, String name, String description,
        String price, String shortCode,
        String stockStatus, String status,
        Long   qrScanCount, Long whatsappClickCount,
        List<ImageDto> images, SellerInfo seller
    ) {}

    public record ImageDto(String imageUrl, Integer displayOrder) {}

    public record SellerInfo(
        Long   id, String shopName, String shopSlug,
        String profileImageUrl, String whatsappNumber,
        String upiId, String category
    ) {}

    @Builder @Getter
    public static class CreateRequest {
        private String              name, description, price, stockStatus;
        private List<MultipartFile> images;
    }

    @Builder @Getter
    public static class UpdateRequest {
        private String name, description, price, stockStatus;
    }
}
