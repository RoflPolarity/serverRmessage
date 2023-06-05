package com.example.rmesaage.Chat;
import java.io.Serializable;
import java.util.ArrayList;
public class Message implements Serializable {
    int id;
    private String messageUser;
    private String text;
    private ArrayList<byte[]> bitMaps;
    private ArrayList<String> paths;
    private String sendTo;
    public Message(String messageUser,String text, int id){
        this.messageUser = messageUser;
        this.text = text;
        this.id = id;
    }

    public Message(int id, String messageUser, String text, ArrayList<byte[]> bitMaps, String sendTo,ArrayList<String> paths) {
        this.id = id;
        this.messageUser = messageUser;
        this.text = text;
        this.bitMaps = bitMaps;
        this.sendTo = sendTo;
        this.paths = paths;
    }

    public Message(String messageUser, String text){
        this.messageUser = messageUser;
        this.text = text;
    }
    public Message(String messageUser, ArrayList<byte[]> bitMaps){
        this.messageUser = messageUser;
        this.bitMaps = bitMaps;
    }

    public String getSendTo() {
        return sendTo;
    }

    public ArrayList<String> getPaths() {
        return paths;
    }

    public void setPaths(ArrayList<String> paths) {
        this.paths = paths;
    }

    public int getId() {return id;}
    public String getMessageUser() {return messageUser;}
    public String getText() {return text;}
    public ArrayList<byte[]> getBitMaps() {return bitMaps;}

    public void setId(int id) {
        this.id = id;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setBitMaps(ArrayList<byte[]> bitMaps) {
        this.bitMaps = bitMaps;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }
}