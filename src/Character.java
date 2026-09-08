public class Character extends VisualAsset {
        private String name;
        private String role;

        public Character(String name, String role, String imagePath) {
            super(imagePath);
            this.name = name;
            this.role = role;
        }
        public String getName() { return name; }
        public String getRole() { return role; }
    }

