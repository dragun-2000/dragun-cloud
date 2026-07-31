package vn.co.cake.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.co.cake.entity.District;
import vn.co.cake.entity.Province;
import vn.co.cake.entity.Ward;
import vn.co.cake.repository.DistrictRepository;
import vn.co.cake.repository.WardRepository;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.utils.MasterUtil;

import java.util.List;

@Controller
public class LocationController {

    private final ProvinceRepository provinceRepository;

    private final DistrictRepository districtRepository;

    private final WardRepository wardRepository;

    private final MasterUtil masterUtil;

    public LocationController(DistrictRepository districtRepository,
                              WardRepository wardRepository, MasterUtil masterUtil, 
                              ProvinceRepository provinceRepository) {
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.masterUtil = masterUtil;
        this.provinceRepository = provinceRepository;
    }

    @GetMapping("/provinces")
    @ResponseBody
    public List<Province> getDistrictsByProvince() {
        return provinceRepository.findAllByDeletedFalseOrderByNameAsc();
    }

    @GetMapping("/districts/{provinceCode}")
    @ResponseBody
    public List<District> getDistrictsByProvince(@PathVariable String provinceCode) {
        return districtRepository.findAllByParentCodeOrderByNameAsc(provinceCode);
    }
    
    @GetMapping("/wards/{districtCode}")
    @ResponseBody
    public List<Ward> getWardByProvince(@PathVariable String districtCode) {
        return wardRepository.findAllByParentCodeOrderByNameAsc(districtCode);
    }

    @GetMapping("/setup/location")
    @ResponseBody
    public void setupLocation() throws Exception {
        masterUtil.importMasterProvince();
    }
}
