package com.software.logistic.dto;


import com.software.logistic.entity.Address;
import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    private String consigneeName;
    private String consigneePhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private boolean isDefault;


    // 可以创建一个静态方法来快速转换
    public static AddressDTO fromEntity(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setConsigneeName(address.getConsigneeName());
        dto.setConsigneePhone(address.getConsigneePhone());
        dto.setProvince(address.getProvince());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setDetailAddress(address.getDetailAddress());
        dto.setDefault(address.getIsDefault());
        return dto;
    }
}