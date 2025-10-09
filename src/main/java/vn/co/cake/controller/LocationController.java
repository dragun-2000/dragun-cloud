package vn.co.cake.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.co.cake.entity.District;
import vn.co.cake.entity.Ward;
import vn.co.cake.repository.DistrictRepository;
import vn.co.cake.repository.WardRepository;
import vn.co.cake.utils.MasterUtil;

import java.util.List;

@Controller
public class LocationController {

    private final DistrictRepository districtRepository;

    private final WardRepository wardRepository;

    private final MasterUtil masterUtil;

    public LocationController(DistrictRepository districtRepository,
                              WardRepository wardRepository, MasterUtil masterUtil) {
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.masterUtil = masterUtil;
    }

    @GetMapping("/districts/{provinceCode}")
    @ResponseBody
    public List<District> getDistrictsByProvince(@PathVariable String provinceCode) {
        return districtRepository.findAllByParentCode(provinceCode);
    }
    
    @GetMapping("/wards/{districtCode}")
    @ResponseBody
    public List<Ward> getWardByProvince(@PathVariable String districtCode) {
        return wardRepository.findAllByParentCode(districtCode);
    }

    @GetMapping("/setup/location")
    @ResponseBody
    public void setupLocation() throws Exception {
        masterUtil.importMasterProvince();
    }
}
