package searcher.shopSearcher.sneakerShop;

import config.Config;
import data.Offer;
import http.ContentSearcher;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searcher.Searcher;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SneakerShopSearcher implements Searcher {

    final String ADDRESS = "https://sneakershop.pl";
    final int TIMEOUT = 5000;
    final String SHOP_NAME = "SneakerShop";

    @Override
    public List<Offer> getOffers(String shoeName, boolean genderMale, String size) {
        ContentSearcher contentSearcher = new ContentSearcher();

        String shoeNameForUrl = shoeName.replace(" ", "+"); // the cannot be whitespace in url
        String addressWithParams = ADDRESS + "/pl/search.html?filter_sizes=760&filter_traits[1343913850]=GENDER" +
                "&filter_pricerange=&text=" + shoeNameForUrl;

        /*
        * filter_traits[1343913850]=86 86 is for women, 87 for men
        * */

        //addressWithParams = addressWithParams.replace("SIZE", ""+getSizeParam(size));
        String gender = genderMale ? "87" : "86";
        addressWithParams = addressWithParams.replace("GENDER", gender);

        //System.out.println(addressWithParams);

        Document doc = null;
        try {
            doc = includeSizeToURl(doc = Jsoup.connect(addressWithParams).get(), addressWithParams, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (doc == null) {
            System.out.println(SHOP_NAME + " doc is null!");
            return List.of();
        }

        return getOffersFromDoc(doc);
    }

    private List<Offer> getOffersFromDoc(Document doc) {
        List<Offer> offers = new ArrayList<>();
        Elements elements = doc.getElementsByClass("product_wrapper_sub");
        elements.forEach(product -> {
            Elements productNameElements = product.getElementsByClass("product-name");
            String productName = productNameElements.get(0).ownText();
            String productAddress = ADDRESS + productNameElements.get(0).attr("href");

            String productPrice = product.getElementsByClass("price").get(0).ownText();
            productPrice = productPrice.replaceAll("zł", "");
            productPrice = productPrice.replaceAll(",", ".");
            productPrice = productPrice.trim();

            String imageUrl = product.getElementsByTag("img").get(0).attr("data-src");

            if (NumberUtils.isCreatable(productPrice)) {
                offers.add(new Offer(productName, new BigDecimal(productPrice), imageUrl, productAddress, SHOP_NAME));
            } else {
                System.out.println(Config.PRICE_IS_NOT_A_CREATABLE_NUMBER + productPrice);
            }
        });
        return offers;
    }

    private Document includeSizeToURl(Document doc, String addressWithParams, String size) {
        try {
            List<Element> liList = doc.getElementsByClass("filter_items_1340356124").get(0).getElementsByTag("li");
            for (Element li : liList) {
                Element wrapper = li.getElementsByClass("filter_name_wrapper").get(0);
                String sizeText = wrapper.getElementsByTag("span").attr("data-filter");
                // example sizeText: 38.5 - 24 cm
                sizeText = sizeText.replace("-", "");
                String sizeFragment = sizeText.substring(0, 4);
                sizeFragment = sizeFragment.trim();
                System.out.println("SIZE:" + sizeFragment);
                if (sizeFragment.equals(size)) {
                    // filter_quantity_1340356124_val758_quantity
                    String encodedSize = wrapper.getElementsByTag("span").attr("id");
                    encodedSize = encodedSize.replaceAll("val", "").split("_")[3];
                    addressWithParams = addressWithParams.replace("760", encodedSize);
                }
            }
            doc = Jsoup.connect(addressWithParams).get();
            System.out.println(addressWithParams);
            return doc;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }

}
