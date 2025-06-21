package g_mungus.genesis.asteroid.generation.composition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class CompositionData {

    @JsonProperty("composition")
    private List<BlockComposition> composition;

    public List<BlockComposition> getComposition() {
        return composition;
    }

    public void setComposition(List<BlockComposition> composition) {
        this.composition = composition;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static CompositionData fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CompositionData.class);
    }

    public static class BlockComposition {

        @JsonProperty("type")
        private String type;

        @JsonProperty("amount")
        private int amount;


        public BlockComposition() {}

        public BlockComposition(String type, int amount) {
            this.type = type;
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }
}

