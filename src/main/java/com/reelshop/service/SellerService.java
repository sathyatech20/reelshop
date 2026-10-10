package com.reelshop.service;

import com.reelshop.entity.Seller;
import com.reelshop.exception.ReelShopException;
import com.reelshop.repository.SellerRepository;
import com.reelshop.util.ShortCodeGenerator;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository    sellerRepository;
    private final ShortCodeGenerator  shortCodeGenerator;
    private final CloudinaryService   cloudinaryService;

    public boolean isSlugAvailable(String slug) {
        return !sellerRepository.existsByShopSlug(slug);
    }

    @Transactional
    public SellerDto setupShop(Long sellerId, SetupRequest req) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> ReelShopException.notFound("Seller"));

        if (sellerRepository.existsByShopSlug(req.shopSlug)) {
            throw ReelShopException.slugTaken(req.shopSlug);
        }

        seller.setShopName(req.shopName);
        seller.setShopSlug(req.shopSlug);
        seller.setWhatsappNumber(req.whatsappNumber);
        seller.setCategory(Seller.ShopCategory.valueOf(req.category));
        seller.setIsSetupComplete(true);

        if (req.upiId   != null) seller.setUpiId(req.upiId);
        if (req.shopBio != null) seller.setShopBio(req.shopBio);

        if (req.profileImage != null && !req.profileImage.isEmpty()) {
            try {
                CloudinaryService.UploadResult result =
                    cloudinaryService.upload(req.profileImage, "profiles");
                seller.setProfileImageUrl(result.url());
                seller.setProfileImagePublicId(result.publicId());
            } catch (Exception e) {
                log.warn("Profile image upload failed, continuing: {}", e.getMessage());
            }
        }

        return toDto(sellerRepository.save(seller));
    }

    public SellerDto getSellerDto(Long sellerId) {
        return toDto(sellerRepository.findById(sellerId)
                .orElseThrow(() -> ReelShopException.notFound("Seller")));
    }

    @Transactional
    public SellerDto updateProfile(Long sellerId, UpdateRequest req) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> ReelShopException.notFound("Seller"));

        if (req.shopName       != null) seller.setShopName(req.shopName);
        if (req.whatsappNumber != null) seller.setWhatsappNumber(req.whatsappNumber);
        if (req.upiId          != null) seller.setUpiId(req.upiId);
        if (req.shopBio        != null) seller.setShopBio(req.shopBio);
        if (req.themeId        != null) seller.setThemeId(req.themeId);

        if (req.profileImage != null && !req.profileImage.isEmpty()) {
            try {
                if (seller.getProfileImagePublicId() != null) {
                    cloudinaryService.delete(seller.getProfileImagePublicId());
                }
                CloudinaryService.UploadResult result =
                    cloudinaryService.upload(req.profileImage, "profiles");
                seller.setProfileImageUrl(result.url());
                seller.setProfileImagePublicId(result.publicId());
            } catch (Exception e) {
                log.warn("Profile image update failed: {}", e.getMessage());
            }
        }

        return toDto(sellerRepository.save(seller));
    }

    public SellerDto toDto(Seller s) {
        return new SellerDto(
            s.getId(), s.getPhoneNumber(), s.getShopName(), s.getShopSlug(),
            s.getShopBio(), s.getProfileImageUrl(),
            s.getCategory() != null ? s.getCategory().name() : null,
            s.getWhatsappNumber(), s.getUpiId(), s.getThemeId(),
            s.getIsSetupComplete(), s.getTotalQrScans(), s.getTotalWhatsappClicks()
        );
    }

    public record SellerDto(
        Long id, String phoneNumber, String shopName, String shopSlug,
        String shopBio, String profileImageUrl, String category,
        String whatsappNumber, String upiId, String themeId,
        Boolean isSetupComplete, Long totalQrScans, Long totalWhatsappClicks
    ) {}

    @Builder @Getter
    public static class SetupRequest {
        private String shopName, shopSlug, category, whatsappNumber, upiId, shopBio;
        private MultipartFile profileImage;
    }

    @Builder @Getter
    public static class UpdateRequest {
        private String shopName, whatsappNumber, upiId, shopBio, themeId;
        private MultipartFile profileImage;
    }
}
