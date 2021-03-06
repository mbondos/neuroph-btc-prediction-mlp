package tk.mbondos;

import javafx.util.Pair;
import org.json.JSONObject;
import tk.mbondos.dl4j.ExchangeRateData;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CoinDeskData {

    private String closePriceUrl = "https://api.coindesk.com/v1/bpi/historical/close.json";
    private String ohlcPriceUrl = "https://api.coindesk.com/v1/bpi/historical/ohlc.json";
    private String filenamePrefix = "";


    public CoinDeskData() {
    }

    /**
     * Get bitcoin price index from CoinDesk api.
     *
     * @param url Url with api query.
     * @return StringBuilder with fetched data.
     */
    private StringBuilder getBpi(URL url) {
        StringBuilder stringBuilder = new StringBuilder(1024);
        String line;
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(0);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            stringBuilder.setLength(0);

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }

        } catch (IOException e) {
            e.printStackTrace(); //TODO custom exception
        }

        return stringBuilder;
    }

    /**
     * Get last 31 days close price.
     *
     * @return Filename of file with parsed data.
     */
    public String getClosePriceLast31Days() {
        URL url = createUrl(String.format("%s?start=%s&end=%s", closePriceUrl, LocalDate.now().minusDays(31), LocalDate.now().minusDays(1)));
        String filename = "data/btc_close_last_31_days.csv";
        writeDataToFile(filename, getBpi(url));
        return filenamePrefix + filename;

    }

    /**
     * Get close price form data range.
     *
     * @param startDate Start date.
     * @param endDate   End date.
     * @return Filename of file with parsed data.
     */
    public String getClosePriceDateRange(LocalDate startDate, LocalDate endDate) {
        URL url = createUrl(String.format("%s?start=%s&end=%s", closePriceUrl, startDate, endDate));
        String filename = "data/btc_close_range.csv";
        writeDataToFile(filename, getBpi(url));
        return filenamePrefix + filename;

    }

    /**
     * Get ohlc price from data range.
     *
     * @param startDate Start date.
     * @param endDate   End date.
     * @return Filename of file with parsed data.
     */
    public String getOhlcPriceDateRange(LocalDate startDate, LocalDate endDate) {
        URL url = createUrl(String.format("%s?start=%s&end=%s", ohlcPriceUrl, startDate, endDate));
        String filename = "data/btc_ohlc_range.csv";
        writeOhlcDataToFile(filename, getBpi(url));
        return filenamePrefix + filename;
    }

    /**
     * Get lifetime close price.
     *
     * @return Filename of file with parsed data.
     */
    public String getClosePriceLifetime() {
        URL url = createUrl(String.format("%s?start=%s&end=%s", closePriceUrl, LocalDate.of(2010, 7, 19), LocalDate.now()));
        String filename = "data/btc_close_lifetime.csv";
        writeDataToFile(filename, getBpi(url));
        return filenamePrefix + filename;
    }

    /**
     * Get lifetime ohlc price.
     *
     * @return Filename of file with parsed data.
     */
    public String getOhlcPriceLifetime() {
        URL url = createUrl(String.format("%s?start=%s&end=%s", ohlcPriceUrl, LocalDate.of(2010, 7, 19), LocalDate.now()));
        String filename = "data/btc_ohlc_lifetime.csv";
        writeOhlcDataToFile(filename, getBpi(url));
        return filenamePrefix + filename;
    }

    /**
     * Write to file. Close price only.
     * Format: date, close
     *
     * @param filename      Save location.
     * @param stringBuilder Fetched data.
     */
    private void writeDataToFile(String filename, StringBuilder stringBuilder) {
        if (stringBuilder.length() == 0) {
            throw new RuntimeException();
        }
        JSONObject jsonObject = new JSONObject(stringBuilder.toString());
        JSONObject bpi = jsonObject.getJSONObject("bpi");

        List<Pair<String, Number>> list = new LinkedList<>();


        String bpiString = bpi.toString();
        bpiString = bpiString.substring(1, bpiString.length() - 1);
        String[] tokens = bpiString.split(",");
        try {
            File file = new File(filenamePrefix + filename);
            file.getParentFile().mkdirs();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));

            String date;
            double value;

            for (int i = 0; i < tokens.length; i++) {
                String[] pair = tokens[i].split(":");
                date = pair[0].substring(1, pair[0].length() - 1);
                value = Double.valueOf(pair[1]);
                list.add(new Pair<>(date, value));
            }

            Collections.sort(list, (o1, o2) -> {
                LocalDate date1 = LocalDate.parse(o1.getKey());
                LocalDate date2 = LocalDate.parse(o2.getKey());
                return date1.compareTo(date2);

            });

            boolean isNotFirstLine = false;
            for (Pair pair :
                    list) {
                if (isNotFirstLine) {
                    bufferedWriter.newLine();
                } else {
                    isNotFirstLine = true;
                }

                bufferedWriter.write(pair.getKey() + "," + pair.getValue());
            }

            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write to file ohlc data.
     * Format: date, open, high, low, close.
     *
     * @param filename      Save location.
     * @param stringBuilder Fetched data.
     */
    private void writeOhlcDataToFile(String filename, StringBuilder stringBuilder) {
        if (stringBuilder.length() == 0) {
            throw new RuntimeException();
        }
        JSONObject jsonObject = new JSONObject(stringBuilder.toString());
        JSONObject bpi = jsonObject.getJSONObject("bpi");

        String bpiString = bpi.toString();
        bpiString = bpiString.substring(1, bpiString.length() - 1);
        String[] tokens = bpiString.split("},");
        List<ExchangeRateData> data = new ArrayList<>();
        try {
            File file = new File(filenamePrefix + filename);
            file.getParentFile().mkdirs();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));

            for (String token : tokens) {
                String date = token.substring(1, 11);
                String ohlcString = token.substring(15, token.length() - 1);
                String[] ohlcArray = ohlcString.split(",");
                int i = 0;
                double[] priceArray = new double[4];
                for (String priceString : ohlcArray) {
                    String[] splitPrice = priceString.split(":");
                    if (splitPrice.length == 2)
                        priceArray[i] = Double.valueOf(splitPrice[1]);
                    i++;
                }
                //format : date, open, high, low, close
                data.add(new ExchangeRateData(date, priceArray[3], priceArray[0], priceArray[1], priceArray[2]));
            }
            Collections.sort(data);
            for (int i = 0; i < data.size(); i++) {
                if (i != 0) {
                    bufferedWriter.newLine();
                }
                ExchangeRateData line = data.get(i);
                bufferedWriter.write(line.toString());
            }

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private URL createUrl(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }

        return url;
    }

}
