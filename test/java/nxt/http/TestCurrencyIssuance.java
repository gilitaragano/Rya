package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.CurrencyType;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestCurrencyIssuance extends BlockchainTest {

    @Test
    public void issueCurrency() {
        issueCurrencyImpl();
    }

    public static String issueCurrencyImpl() {
        return issueCurrencyImpl(CurrencyType.EXCHANGEABLE.getCode(), 0, 0);
    }

    public static String issueCurrencyImpl(byte type, int issuanceHeight, long minReservePerUnitNQT) {
        return issueCurrencyImpl(type, issuanceHeight, minReservePerUnitNQT, 100000, 100000, (byte)0, (byte)0, (byte)0);
    }

    public static String issueCurrencyImpl(byte type, int issuanceHeight, long minReservePerUnitNQT,
                                           long totalSupply, long initialSupply, byte minDiff, byte maxDiff, byte algorithm) {
        APICall apiCall = new APICall.Builder("issueCurrency").
                secretPhrase(secretPhrase1).
                feeNQT(1000 * Constants.ONE_NXT).
                param("name", "Test1").
                param("code", "TSX").
                param("code", "TSX").
                param("description", "Test Currency 1").
                param("type", type).
                param("totalSupply", totalSupply).
                param("initialSupply", initialSupply).
                param("issuanceHeight", issuanceHeight).
                param("minReservePerUnitNQT", minReservePerUnitNQT).
                param("minDifficulty", minDiff).
                param("maxDifficulty", maxDiff).
                param("algorithm", algorithm).
                build();

        JSONObject issueCurrencyResponse = apiCall.invoke();
        String currencyId = (String) issueCurrencyResponse.get("transaction");
        Logger.logMessage("issueCurrencyResponse: " + issueCurrencyResponse.toJSONString());
        generateBlock();

        apiCall = new APICall.Builder("getCurrency").param("currency", currencyId).build();
        JSONObject getCurrencyResponse = apiCall.invoke();
        Logger.logMessage("getCurrencyResponse:" + getCurrencyResponse.toJSONString());
        Assert.assertEquals(currencyId, getCurrencyResponse.get("currency"));
        Assert.assertEquals("TSX", getCurrencyResponse.get("code"));
        return currencyId;
    }
}
