package lambdabitcoinpricealert;

public class BitcoinPrice {

    private PriceData data;

    public BitcoinPrice() {
    }

    public BitcoinPrice(PriceData data) {
        this.data = data;
    }

    public PriceData getData() {
        return data;
    }

    public void setData(PriceData data) {
        this.data = data;
    }

    class PriceData {
        private double amount;
        private String base;
        private String currency;

        public PriceData() {
        }

        public PriceData(double amount, String base, String currency) {
            this.amount = amount;
            this.base = base;
            this.currency = currency;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        @Override
        public String toString() {
            return String.format("base: %s, currency: %s, amount: %f", this.base, this.currency, this.amount);
        }
    }
}
