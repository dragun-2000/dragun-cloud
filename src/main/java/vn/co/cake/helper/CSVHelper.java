package vn.co.cake.helper;

import org.springframework.web.multipart.MultipartFile;

public class CSVHelper {
  public static String TYPE = "text/csv";
  static String[] HEADER = { "name", "email"};

  public static boolean hasCSVFormat(MultipartFile file) {
    return TYPE.equals(file.getContentType());
  }
}