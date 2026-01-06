package vn.co.cake.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SizeAdviceMessage {
    private String recommendedSize;
    private String backupSize;
    private List<String> reason;
    private String productFitComment;
    private String closingQuestion;
}
