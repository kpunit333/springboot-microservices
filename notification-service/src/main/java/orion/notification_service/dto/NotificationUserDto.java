package orion.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationUserDto {

    @JsonProperty("id")
    @JsonAlias({ "id", "userId", "user_id" })
    private String id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    @JsonAlias({ "phone", "phoneNumber", "phone_number", "mobile" })
    private String phone;

    @JsonProperty("countryCode")
    @JsonAlias({ "countryCode", "country_code" })
    private String countryCode;

    @JsonProperty("whatsapp")
    @JsonAlias({ "whatsapp", "whatsappNumber", "whatsapp_number" })
    private String whatsapp;
}

