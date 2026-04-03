package com.davidr.secureft.datamodels;

public class SignalMessage {
    private String type;
    private String roomId;
    private String from;
    private String to;
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;

    public SignalMessage() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }
}