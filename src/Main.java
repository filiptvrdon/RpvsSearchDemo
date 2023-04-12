import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private record Partner(Integer id, String ico, String menoNazov, String adresa) {

        @Override
        public String toString() {
            return "IČO='" + ico + '\'' + "\n" +
                    "Meno partnera='" + menoNazov + '\'' + "\n" +
                    "Adresa='" + adresa + '\'' + "\n";
        }

        public Integer id() {
            return id;
        }
    }

    private record KonecnyUzivatelVyhod(int id, String meno, String priezvisko, String adresa) {

        @Override
        public String toString() {
            return "Meno ='" + meno + '\'' + "\n" +
                    "Priezvisko ='" + priezvisko + '\'' + "\n" +
                    "Adresa ='" + adresa + '\'' + "\n";
        }
    }


    private final static String PARTNERS_API_URL = "https://rpvs.gov.sk/rpvs/Partner/Partner/GetPartners?text=";
    private final static String PARTNER_DETAILS_BY_ID_API_URL = "https://rpvs.gov.sk/OpenData/Partneri({partnerId})?$expand=KonecniUzivateliaVyhod";
    private final static String KONECNY_UZIVATEL_VYHOD_BY_ID_API_URL = "https://rpvs.gov.sk/OpenData/KonecniUzivateliaVyhod({kuvId})?$expand=Adresa";

    private final static String MENO_PARTNERA = "MenoPartnera";
    private final static String ICO = "Ico";
    private final static String ADRESA = "Adresa";
    private static final String PARTNER_ID = "PartnerId";
    private static final List<Partner> partnerList = new ArrayList<>();

    public static void main(String[] args) {
        while (true) {
            String userInput = getUserInput();

            if (userInput.equalsIgnoreCase("q")) {
                System.out.println("Ukončujem program.");
                break;
            }

            getPartnersList(userInput);

            if (partnerList.size() > 0) {
                List<KonecnyUzivatelVyhod> konecniUzivateliaVyhod = new ArrayList<>();

                for (Partner partner : partnerList) {
                    try {
                        konecniUzivateliaVyhod = getKonecniUzivateliaVyhod(partner);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                    System.out.println(partner);
                    System.out.println("Koneční užívatelia výhod: ");
                    for (KonecnyUzivatelVyhod kuv : konecniUzivateliaVyhod) {
                        System.out.println(kuv);
                    }
                    System.out.println("--------------------------------------------------");

                }

            } else {
                System.out.println("Nenašli sa žiadni partneri.");
            }
        }
    }

    private static String getUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Zadajte časť alebo celý názov/IČO partnera pre vyhľadávanie. (Zadajte 'q' pre ukončenie programu) ");
        return scanner.nextLine();
    }

    private static void getPartnersList(String userInput) {
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = getJsonArrayFromApiCall(new URL(PARTNERS_API_URL + userInput));
//            System.out.println(jsonArray);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Partner partner = new Partner(jsonObject.getInt(PARTNER_ID), jsonObject.getString(ICO), jsonObject.getString(MENO_PARTNERA), jsonObject.getString(ADRESA));
            partnerList.add(partner);
        }
    }

    private static List<KonecnyUzivatelVyhod> getKonecniUzivateliaVyhod(Partner partner) throws MalformedURLException {
        List<KonecnyUzivatelVyhod> konecnyUzivatelVyhodList = new ArrayList<>();

        JSONObject partnerDetails = getJsonObjectFromApiCall(new URL(PARTNER_DETAILS_BY_ID_API_URL.replace("{partnerId}", partner.id().toString())));
        JSONArray partnersKUV = partnerDetails.getJSONArray("KonecniUzivateliaVyhod");

        for (int i = 0; i < partnersKUV.length(); i++) {
            int kuvId = partnersKUV.getJSONObject(i).getInt("Id");

            JSONObject kuvDetails = getJsonObjectFromApiCall(new URL(KONECNY_UZIVATEL_VYHOD_BY_ID_API_URL.replace("{kuvId}", String.valueOf(kuvId))));
            KonecnyUzivatelVyhod konecnyUzivatelVyhod = new KonecnyUzivatelVyhod(kuvId, kuvDetails.getString("Meno"), kuvDetails.getString("Priezvisko"), kuvDetails.getJSONObject("Adresa").toString());
            konecnyUzivatelVyhodList.add(konecnyUzivatelVyhod);
        }

        return konecnyUzivatelVyhodList;
    }

    private static JSONArray getJsonArrayFromApiCall(URL url) {
//        System.out.println("API call: " + url.toString());
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                Scanner responseScanner = new Scanner(url.openStream());
                StringBuilder responseStringBuilder = new StringBuilder();
                while (responseScanner.hasNext()) {
                    responseStringBuilder.append(responseScanner.nextLine());
                }
                responseScanner.close();

                String jsonString = responseStringBuilder.toString();

                if (jsonString.startsWith("[")) {
                    return new JSONArray(jsonString);
                } else {
                    return new JSONArray().put(new JSONObject(jsonString));
                }

            } else {
                System.out.println("Nastala chyba pri komunikácii so serverom.");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return new JSONArray();
    }

    private static JSONObject getJsonObjectFromApiCall(URL url) {
        JSONArray jsonArray = getJsonArrayFromApiCall(url);
//        System.out.println("JSON ARRAY");
//        System.out.println(jsonArray);

        return jsonArray.getJSONObject(0);
    }
}
