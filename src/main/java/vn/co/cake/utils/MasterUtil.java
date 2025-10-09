package vn.co.cake.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import vn.co.cake.dto.DistrictDto;
import vn.co.cake.dto.ProvinceDto;
import vn.co.cake.dto.WardDto;
import vn.co.cake.entity.District;
import vn.co.cake.entity.Province;
import vn.co.cake.entity.Ward;
import vn.co.cake.repository.DistrictRepository;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.repository.WardRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MasterUtil {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;

    public MasterUtil(ProvinceRepository provinceRepository, DistrictRepository districtRepository, WardRepository wardRepository) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
    }

    @Transactional
    public void importMasterProvince() throws Exception {
        String path = "D:\\debase-fake\\src\\main\\resources\\province.json";
        Map<String, ProvinceDto> stringProvinceMap = this.readProvinceJson(path);

//        if (CollectionUtils.isEmpty(provinceRepository.findAll())) {
//            List<Province> provinces = new ArrayList<>();
//            stringProvinceMap.forEach((key, provinceDto) -> {
//                Province province = new Province();
//                province.setCode(provinceDto.getCode());
//                province.setType(provinceDto.getType());
//                province.setName(provinceDto.getName());
//                province.setSlug(provinceDto.getSlug());
//                province.setNameWithType(provinceDto.getName_with_type());
//                provinces.add(province);
//            });
//
//            provinceRepository.saveAll(provinces);
//            System.out.println("province");
//        }
//
//
//        String pathDistrict = "D:\\debase-fake\\src\\main\\resources\\districts.json";
//        Map<String, DistrictDto> stringDistrictDtoMap = this.readDistrictJson(pathDistrict);
//        List<District> districts = new ArrayList<>();
//        if (CollectionUtils.isEmpty(districtRepository.findAll())) {
//            stringDistrictDtoMap.forEach((key, districtDto) -> {
//                District district = new District();
//                district.setCode(districtDto.getCode());
//                district.setType(districtDto.getType());
//                district.setName(districtDto.getName());
//                district.setSlug(districtDto.getSlug());
//                district.setNameWithType(districtDto.getName_with_type());
//                district.setPath(districtDto.getPath());
//                district.setPathWithType(districtDto.getPath_with_type());
//                district.setParentCode(districtDto.getParent_code());
//                districts.add(district);
//            });
//            districtRepository.saveAll(districts);
//
//            System.out.println("district");
//        }

        String pathWard = "D:\\debase-fake\\src\\main\\resources\\ward.json";
        Map<String, WardDto> stringWardDtoMap = this.readWardJson(pathWard);
        List<Ward> wards = new ArrayList<>();

        if (CollectionUtils.isEmpty(wardRepository.findAll())) {
            stringWardDtoMap.forEach((key, wardDto) -> {
                Ward ward = new Ward();
                ward.setCode(wardDto.getCode());
                ward.setType(wardDto.getType());
                ward.setName(wardDto.getName());
                ward.setSlug(wardDto.getSlug());
                ward.setNameWithType(wardDto.getName_with_type());
                ward.setPath(wardDto.getPath());
                ward.setPathWithType(wardDto.getPath_with_type());
                ward.setParentCode(wardDto.getParent_code());
                wards.add(ward);
            });
            wardRepository.saveAll(wards);

            System.out.println("ward");
        }
    }

    private Map<String, ProvinceDto> readProvinceJson(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), new TypeReference<>() {});
    }

    private Map<String, DistrictDto> readDistrictJson(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), new TypeReference<>() {});
    }

    private Map<String, WardDto> readWardJson(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), new TypeReference<>() {});
    }
}
