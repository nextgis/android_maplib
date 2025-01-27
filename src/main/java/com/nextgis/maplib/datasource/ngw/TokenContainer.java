package com.nextgis.maplib.datasource.ngw;

public class TokenContainer {

    public String token;
    public final int responseCode;

    public TokenContainer(final String token, final int responseCode){
        this.token = token;
        this.responseCode = responseCode;
    }

    public void setToken(final String item){
        token = item;
    }


}
