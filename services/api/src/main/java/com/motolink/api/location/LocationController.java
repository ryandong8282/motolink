package com.motolink.api.location;

import com.motolink.api.auth.AuthInterceptor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/me")
    LocationService.MemberLocation update(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId,
            @RequestAttribute(AuthInterceptor.NICKNAME_ATTRIBUTE) String nickname,
            @Valid @RequestBody LocationRequest request) {
        return locationService.update(userId, nickname, request.toUpdate(), null);
    }

    @GetMapping("/nearby")
    List<LocationService.NearbyUser> nearby(
            @RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) String userId,
            @RequestParam
            @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
            double latitude,
            @RequestParam
            @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
            double longitude,
            @RequestParam(defaultValue = "3000")
            @Min(100) @Max(10000)
            int radiusMeters) {
        return locationService.nearby(userId, latitude, longitude, radiusMeters);
    }

    public record LocationRequest(
            @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
            @DecimalMax(value = "90.0", message = "纬度不能大于 90")
            double latitude,
            @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
            @DecimalMax(value = "180.0", message = "经度不能大于 180")
            double longitude,
            @DecimalMin(value = "0.0", message = "精度不能为负数")
            Double accuracy,
            @DecimalMin(value = "0.0", message = "速度不能为负数")
            Double speed,
            @DecimalMin(value = "0.0", message = "方向不能小于 0")
            @DecimalMax(value = "360.0", message = "方向不能大于 360")
            Double bearing) {

        LocationService.LocationUpdate toUpdate() {
            return new LocationService.LocationUpdate(
                    latitude,
                    longitude,
                    accuracy,
                    speed,
                    bearing);
        }
    }
}
