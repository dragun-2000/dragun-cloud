package vn.co.cake.controller.AM009;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.entity.Province;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.security.admin.AdminLoginInfo;

import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@Controller
@Slf4j
public class AM009Controller extends BaseController {
    
    private final ProvinceRepository provinceRepository;

    public AM009Controller(ProvinceRepository provinceRepository) {
        this.provinceRepository = provinceRepository;
    }

    /**
     * List provinces for setting active/inactive
     *
     * @param model model
     * @param session session
     * @return provinces management screen
     */
    @GetMapping(RequestPathConst.AM009)
    public String listProvinces(Model model, HttpSession session, HttpServletResponse response) throws IOException {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            response.sendRedirect(baseUrl.concat(RequestPathConst.AM001_LOGIN));
            return null;
        }

        List<Province> provinces = provinceRepository.findAll();
        model.addAttribute("provinces", provinces);
        addSideMenu(model, RequestPathConst.AM009);
        return ScreenPathConst.AM009_SCREEN;
    }

    /**
     * Update province deleted status
     *
     * @param provinceId province id
     * @param deleted deleted status
     * @return response message
     */
    @PostMapping(RequestPathConst.AM009_UPDATE_STATUS)
    @ResponseBody
    public ResponseEntity<String> updateProvinceStatus(@RequestParam Long provinceId, 
                                                         @RequestParam Boolean deleted) {
        try {
            log.info("Received update request - provinceId: {}, deleted: {}", provinceId, deleted);
            
            if (provinceId == null) {
                log.warn("Province ID is null");
                return new ResponseEntity<>("Province ID không được để trống", HttpStatus.BAD_REQUEST);
            }
            if (deleted == null) {
                log.warn("Deleted status is null for provinceId: {}", provinceId);
                return new ResponseEntity<>("Trạng thái deleted không được để trống", HttpStatus.BAD_REQUEST);
            }
            
            Province province = provinceRepository.findById(provinceId)
                    .orElseThrow(() -> new RuntimeException("Province không tồn tại với ID: " + provinceId));
            
            log.info("Updating province {} (ID: {}) from deleted={} to deleted={}", 
                    province.getName(), provinceId, province.getDeleted(), deleted);
            
            province.setDeleted(deleted);
            provinceRepository.save(province);
            
            log.info("Successfully updated province {} (ID: {}) to deleted={}", 
                    province.getName(), provinceId, deleted);
            
            return new ResponseEntity<>("Cập nhật thành công", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error updating province status - provinceId: {}, error: {}", provinceId, e.getMessage(), e);
            return new ResponseEntity<>("Cập nhật thất bại: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Update all provinces deleted status
     *
     * @param deleted deleted status for all provinces
     * @return response message
     */
    @PostMapping(RequestPathConst.AM009_UPDATE_ALL_STATUS)
    @ResponseBody
    @Transactional
    public ResponseEntity<String> updateAllProvincesStatus(@RequestParam Boolean deleted) {
        try {
            log.info("Received update all request - deleted: {}", deleted);
            
            if (deleted == null) {
                log.warn("Deleted status is null for update all request");
                return new ResponseEntity<>("Trạng thái deleted không được để trống", HttpStatus.BAD_REQUEST);
            }
            
            List<Province> provinces = provinceRepository.findAll();
            log.info("Updating {} provinces to deleted={}", provinces.size(), deleted);
            
            for (Province province : provinces) {
                province.setDeleted(deleted);
            }
            provinceRepository.saveAll(provinces);
            
            log.info("Successfully updated all {} provinces to deleted={}", provinces.size(), deleted);
            
            return new ResponseEntity<>("Cập nhật tất cả thành công", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error updating all provinces status: {}", e.getMessage(), e);
            return new ResponseEntity<>("Cập nhật thất bại: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
