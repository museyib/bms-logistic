package az.inci.bmslogistic.model;

import lombok.Data;

@Data
public class User
{

    private String id;
    private String password;
    private String name;
    private String pickGroup;
    private boolean collectFlag;
    private boolean pickFlag;
    private boolean checkFlag;
    private boolean countFlag;
    private boolean locationFlag;
    private boolean packFlag;
    private boolean docFlag;
    private boolean loadingFlag;
}
