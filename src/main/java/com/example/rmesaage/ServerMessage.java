package com.example.rmesaage;

import java.io.Serializable;


public class ServerMessage<T> implements Serializable {
    private String comma, serverName,Key;
    private T data;

    public ServerMessage(String comma, String serverName, T data, String Key) {
        this.comma = comma;
        this.serverName = serverName;
        this.data = data;
        this.Key = Key;
    }

    public String getKey() {
        return Key;
    }

    public void setKey(String key) {
        Key = key;
    }

    public String getComma() {
        return comma;
    }

    public void setComma(String comma) {
        this.comma = comma;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
