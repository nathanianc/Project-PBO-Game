public class Background extends VisualAsset {
        private String name;
        public Background(String name, String imagePath) {
            super(imagePath);
            this.name = name;
        }
        public String getName() { return name; }
    }

