package pi_man.receiver.world.item.component;

public interface Property {
    Property EMPTY = new Property() {
        @Override
        public float getAsFloat() {
            return 0;
        }

        @Override
        public String getAsString() {
            return "";
        }
    };
    float getAsFloat();
    String getAsString();
}
