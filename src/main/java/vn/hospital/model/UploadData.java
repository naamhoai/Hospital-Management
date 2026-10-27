package vn.hospital.model;

public record UploadData(String filename, String contentType, byte[] bytes) {}
