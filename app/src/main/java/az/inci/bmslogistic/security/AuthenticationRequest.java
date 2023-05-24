package az.inci.bmslogistic.security;


import lombok.Data;

@Data
public class AuthenticationRequest
{
    private String username;
    private String password;
    private String secretKey;
}
