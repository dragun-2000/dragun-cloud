package vn.co.cake.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.co.cake.dto.DistrictDto;
import vn.co.cake.dto.ProvinceDto;
import vn.co.cake.dto.WardDto;
import vn.co.cake.entity.District;
import vn.co.cake.entity.Province;
import vn.co.cake.entity.Ward;
import vn.co.cake.repository.DistrictRepository;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.repository.WardRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class MasterUtil {

    private static final String CLASSPATH_PROVINCE_JSON = "province.json";
    private static final String CLASSPATH_DISTRICTS_JSON = "districts.json";
    private static final String CLASSPATH_WARDS_JSON = "ward.json";
    private static final int SAVE_BATCH_SIZE = 500;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;

    public MasterUtil(ProvinceRepository provinceRepository,
                      DistrictRepository districtRepository,
                      WardRepository wardRepository) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
    }

    /**
     * Import province → districts → wards từ classpath (chỉ khi bảng tương ứng đang trống).
     */
    @Transactional
    public void importMasterProvince() throws Exception {
        if (provinceRepository.count() == 0) {
            Map<String, ProvinceDto> provinceMap = readJsonMap(CLASSPATH_PROVINCE_JSON, new TypeReference<>() {});
            List<Province> provinces = new ArrayList<>(provinceMap.size());
            provinceMap.forEach((key, dto) -> provinces.add(toProvince(dto)));
            provinceRepository.saveAll(provinces);
            log.info("Imported {} provinces from {}", provinces.size(), CLASSPATH_PROVINCE_JSON);
        } else {
            log.info("Skip province import: table already has data");
        }

        if (districtRepository.count() == 0) {
            Map<String, DistrictDto> districtMap = readJsonMap(CLASSPATH_DISTRICTS_JSON, new TypeReference<>() {});
            List<District> districts = new ArrayList<>(districtMap.size());
            districtMap.forEach((key, dto) -> districts.add(toDistrict(dto)));
            saveInBatches(districts, districtRepository::saveAll);
            log.info("Imported {} districts from {}", districts.size(), CLASSPATH_DISTRICTS_JSON);
        } else {
            log.info("Skip districts import: table already has data");
        }

        if (wardRepository.count() == 0) {
            Map<String, WardDto> wardMap = readJsonMap(CLASSPATH_WARDS_JSON, new TypeReference<>() {});
            List<Ward> wards = new ArrayList<>(wardMap.size());
            wardMap.forEach((key, dto) -> wards.add(toWard(dto)));
            saveInBatches(wards, wardRepository::saveAll);
            log.info("Imported {} wards from {}", wards.size(), CLASSPATH_WARDS_JSON);
        } else {
            log.info("Skip wards import: table already has data");
        }
    }

    private Province toProvince(ProvinceDto dto) {
        Province province = new Province();
        province.setCode(dto.getCode());
        province.setType(dto.getType());
        province.setName(dto.getName());
        province.setSlug(dto.getSlug());
        province.setNameWithType(dto.getName_with_type());
        province.setDeleted(false);
        return province;
    }

    private District toDistrict(DistrictDto dto) {
        District district = new District();
        district.setCode(dto.getCode());
        district.setType(dto.getType());
        district.setName(dto.getName());
        district.setSlug(dto.getSlug());
        district.setNameWithType(dto.getName_with_type());
        district.setPath(dto.getPath());
        district.setPathWithType(dto.getPath_with_type());
        district.setParentCode(dto.getParent_code());
        return district;
    }

    private Ward toWard(WardDto dto) {
        Ward ward = new Ward();
        ward.setCode(dto.getCode());
        ward.setType(dto.getType());
        ward.setName(dto.getName());
        ward.setSlug(dto.getSlug());
        ward.setNameWithType(dto.getName_with_type());
        ward.setPath(dto.getPath());
        ward.setPathWithType(dto.getPath_with_type());
        ward.setParentCode(dto.getParent_code());
        return ward;
    }

    private <T> Map<String, T> readJsonMap(String classpathResource, TypeReference<Map<String, T>> typeReference)
            throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathResource);
        if (!resource.exists()) {
            throw new IllegalStateException("Classpath resource not found: " + classpathResource);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    private <E> void saveInBatches(List<E> entities, Consumer<List<E>> saveAll) {
        for (int i = 0; i < entities.size(); i += SAVE_BATCH_SIZE) {
            int end = Math.min(i + SAVE_BATCH_SIZE, entities.size());
            saveAll.accept(entities.subList(i, end));
        }
    }
}
