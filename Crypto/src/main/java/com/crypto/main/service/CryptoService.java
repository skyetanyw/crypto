package com.crypto.main.service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.crypto.main.controller.*;

@Service
public class CryptoService {
	
	static final String DATABASE_URL = "", DATABASE_USER = "", DATABASE_PASSWORD = "";

	static final String ETHEREUM_SYMBOL = "ETHUSDT", BITCOIN_SYMBOL = "BTCUSDT";
	
	@Scheduled(fixedRate = 10000)
	public void updateBestPricing() {
		String binanceInputString = retrievePricingViaURL("https://api.binance.com/api/v3/ticker/bookTicker");
		String houbiInputString = retrievePricingViaURL("https://api.huobi.pro/market/tickers");
		updatePricing(binanceInputString, houbiInputString);
	}
	
	private static String retrievePricingViaURL(String urlString) {
		
		// Attempt to retrieve input string via given URL.
	    StringBuilder stringBuilder = new StringBuilder(); 
		try {
			URL url = new URL(urlString);
			URLConnection urlConnection = url.openConnection();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));  
			String line;
			while ((line = bufferedReader.readLine()) != null) {  
				stringBuilder.append(line + "\n");  
			}  
			bufferedReader.close();  
	    } catch (Exception exception) {  
	      exception.printStackTrace();  
	    }
		
	    return stringBuilder.toString(); 
	}
	
	private static void updatePricing(String binanceInputString, String houbiInputString) {

		// Attempt to parse Binance input string into JSON array and retrieve relevant cryptocurrencies bid & ask price.
		double ethereumBidPrice = 0, ethereumAskPrice = 0, bitcoinBidPrice = 0, bitcoinAskPrice = 0;
		try {
			JSONArray jsonArray = new JSONArray(binanceInputString);
			boolean foundEthereum = false, foundBitcoin = false;
			for (int i = 0, n = jsonArray.length(); i < n; i++) {
			    JSONObject jsonObject = jsonArray.getJSONObject(i);
			    if (jsonObject.getString("symbol").equalsIgnoreCase(ETHEREUM_SYMBOL)) {
			    	ethereumBidPrice = jsonObject.getDouble("bidPrice");
			    	ethereumAskPrice = jsonObject.getDouble("askPrice");
			    	foundEthereum = true;
			    }
			    if (jsonObject.getString("symbol").equalsIgnoreCase(BITCOIN_SYMBOL)) {
			    	bitcoinBidPrice = jsonObject.getDouble("bidPrice");
			    	bitcoinAskPrice = jsonObject.getDouble("askPrice");
			    	foundBitcoin = true;
			    }
			    if (foundEthereum && foundBitcoin)
			    	break;
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
		// Attempt to parse Houbi input string into JSON object and retrieve relevant cryptocurrencies bid & ask price, replacing previous values if it has the better pricing.
		try {
			JSONArray jsonArray = new JSONObject(houbiInputString).getJSONArray("data");
			for (int i = 0, n = jsonArray.length(); i < n; i++) {
			    JSONObject jsonObject = jsonArray.getJSONObject(i);
				boolean foundEthereum = false, foundBitcoin = false;
			    if (jsonObject.getString("symbol").equalsIgnoreCase(ETHEREUM_SYMBOL)) {
			    	ethereumBidPrice = jsonObject.getDouble("bid") < ethereumBidPrice ? ethereumBidPrice : jsonObject.getDouble("bid");
			    	ethereumAskPrice = ethereumAskPrice != 0 && jsonObject.getDouble("ask") > ethereumAskPrice ? ethereumAskPrice : jsonObject.getDouble("ask");
			    	foundEthereum = true;
			    }
			    if (jsonObject.getString("symbol").equalsIgnoreCase(BITCOIN_SYMBOL)) {
			    	bitcoinBidPrice = jsonObject.getDouble("bid") < bitcoinBidPrice ? bitcoinBidPrice : jsonObject.getDouble("bid");
			    	bitcoinAskPrice = bitcoinAskPrice != 0 && jsonObject.getDouble("ask") > bitcoinAskPrice ? bitcoinAskPrice : jsonObject.getDouble("ask");
			    	foundBitcoin = true;
			    }
			    if (foundEthereum && foundBitcoin)
			    	break;
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
		// Update relevant cryptocurrencies ask & bid price in database.
		Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            preparedStatement = connection.prepareStatement("UPDATE CRYPTOCURRENCY SET BID_PRICE = ?, ASK_PRICE = ? WHERE SYMBOL = ?");
            preparedStatement.setDouble(1, ethereumBidPrice);
            preparedStatement.setDouble(2, ethereumAskPrice);
            preparedStatement.setString(3, ETHEREUM_SYMBOL);
            preparedStatement.executeUpdate();
        } catch (Exception exception) {
        	exception.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
        }
		connection = null;
        preparedStatement = null;
        try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            preparedStatement = connection.prepareStatement("UPDATE CRYPTOCURRENCY SET BID_PRICE = ?, ASK_PRICE = ? WHERE SYMBOL = ?");
            preparedStatement.setDouble(1, bitcoinBidPrice);
            preparedStatement.setDouble(2, bitcoinAskPrice);
            preparedStatement.setString(3, BITCOIN_SYMBOL);
            preparedStatement.executeUpdate();
        } catch (Exception exception) {
        	exception.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
        }
	}
	
	public static String getLatestPrice() {
		
		// Retrieve cryptocurrency records from database and construct JSON object based on results.
		JSONArray jsonArray = new JSONArray();
		Connection connection = null;
        Statement statement = null;
		try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            statement = connection.createStatement();
    		ResultSet resultSet = statement.executeQuery("SELECT SYMBOL, BID_PRICE, ASK_PRICE FROM CRYPTOCURRENCY");
    		while (resultSet.next()) {
    			JSONObject jsonObject = new JSONObject();
    			jsonObject.put("symbol", resultSet.getString("SYMBOL"));
    			jsonObject.put("bidPrice", resultSet.getDouble("BID_PRICE"));
    			jsonObject.put("askPrice", resultSet.getDouble("ASK_PRICE"));
    			jsonArray.put(jsonObject);
    		}
    		resultSet.close();
        } catch (Exception exception) {
        	exception.printStackTrace();
        } finally {
            try {
                if (statement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            	sqlException.printStackTrace();
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
        }
		return jsonArray.toString();
	}
	
	public static String trade(int userId, String type, String symbol, int quantity) {
		
		// Perform initial validation.
		if (!type.equalsIgnoreCase("BUY") && !type.equalsIgnoreCase("SELL")) {
			return "Invalid order.";
		} else if (!symbol.equalsIgnoreCase(ETHEREUM_SYMBOL) && !symbol.equalsIgnoreCase(BITCOIN_SYMBOL)) {
			return "Invalid symbol.";
		} else if (userId < 0) {
			return "Invalid user.";
		} else if (quantity < 0) {
			return "Invalid quantity.";
		}
		
		// Initialize a SQL connection without auto commit and check for user's balance sufficiency if buying.
		String error = null;
		Connection connection = null;
        PreparedStatement selectPreparedStatement = null, insertPreparedStatement = null, updatePreparedStatement = null;
		try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            connection.setAutoCommit(false);
            if (type.equalsIgnoreCase("BUY")) {
	            selectPreparedStatement = connection.prepareStatement("SELECT BALANCE " + (type.equalsIgnoreCase("SELL") ? "+" : "-") + " (? * (SELECT " + (type.equalsIgnoreCase("SELL") ? "BID_PRICE" : "ASK_PRICE") + " FROM CRYPTOCURRENCY WHERE SYMBOL = ?)) FROM USERS WHERE USERS_ID = ?");
	            selectPreparedStatement.setInt(1, quantity);
	            selectPreparedStatement.setString(2, symbol);
	            selectPreparedStatement.setInt(3, userId);
	            ResultSet resultSet = selectPreparedStatement.executeQuery();
	    		if (resultSet.next()) {
	    			if (resultSet.getDouble(1) < 0) {
	    				error = "Insufficient balance.";
	    			}
	    		}
	    		else {
	    			error = "Invalid user.";
	    		}
	    		resultSet.close();
            }
    		// Check for user's cryptocurrency balance sufficiency if sell.
            else if (type.equalsIgnoreCase("SELL")) {
            	selectPreparedStatement = connection.prepareStatement("SELECT SUM(QUANTITY) - ? FROM ORDERS WHERE SYMBOL = ? AND USERS_ID = ?");
	            selectPreparedStatement.setInt(1, quantity);
	            selectPreparedStatement.setString(2, symbol);
	            selectPreparedStatement.setInt(3, userId);
	            ResultSet resultSet = selectPreparedStatement.executeQuery();
	    		if (resultSet.next()) {
	    			if (resultSet.getInt(1) < 0) {
	    				error = "Insufficient cryptocurrency balance.";
	    			}
	    		}
	    		resultSet.close();
            }
    		if (error != null) {
    			return error;
    		}
    		
    		// Proceed to add order record for user.
    		insertPreparedStatement = connection.prepareStatement("INSERT INTO ORDERS VALUES ((SELECT MAX(ORDERS_ID) + 1 FROM ORDERS), ?, ?, ?, (SELECT " + (type.equalsIgnoreCase("SELL") ? "BID_PRICE" : "ASK_PRICE") + " FROM CRYPTOCURRENCY WHERE SYMBOL = ?), ?)");
    		insertPreparedStatement.setInt(1, userId);
    		insertPreparedStatement.setString(2, symbol);
    		insertPreparedStatement.setInt(3, type.equalsIgnoreCase("BUY") ? quantity : -1 * quantity);
    		insertPreparedStatement.setString(4, symbol);
    		insertPreparedStatement.setDate(5, java.sql.Date.valueOf(java.time.LocalDate.now()));
            insertPreparedStatement.execute();
            
            // Proceed to update balance for user.
            updatePreparedStatement = connection.prepareStatement("UPDATE USERS SET BALANCE = BALANCE " + (type.equalsIgnoreCase("SELL") ? "+" : "-") + " (? * (SELECT " + (type.equalsIgnoreCase("SELL") ? "BID_PRICE" : "ASK_PRICE") + " FROM CRYPTOCURRENCY WHERE SYMBOL = ?)) WHERE USERS_ID = ?");
            updatePreparedStatement.setInt(1, quantity);
            updatePreparedStatement.setString(2, symbol);
            updatePreparedStatement.setInt(3, userId);
            updatePreparedStatement.executeUpdate();
            
    		connection.commit();
        } catch (Exception exception) {
        	exception.printStackTrace();
        	if (connection != null) {
                try {
					connection.rollback();
				} catch (SQLException sqlException) {
				}
            }
        	error = "Unknown error. Please try again.";
        } finally {
            try {
                if (selectPreparedStatement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
            try {
                if (insertPreparedStatement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
            try {
                if (updatePreparedStatement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
        }
		
		return error != null && !error.isEmpty() ? error : "Successfully saved trade.";
	}
	
	public static String getUserCrypto(int userId) {
		
		// Retrieve cryptocurrency records from database and construct JSON object based on results.
		JSONArray jsonArray = new JSONArray();
		Connection connection = null;
        PreparedStatement preparedStatement = null;
		try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            preparedStatement = connection.prepareStatement("SELECT SYMBOL, SUM(QUANTITY) FROM ORDERS WHERE USERS_ID = ? GROUP BY SYMBOL");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
    		while (resultSet.next()) {
    			JSONObject jsonObject = new JSONObject();
    			jsonObject.put("symbol", resultSet.getString("SYMBOL"));
    			jsonObject.put("quantity", resultSet.getDouble(2));
    			jsonArray.put(jsonObject);
    		}
    		resultSet.close();
        } catch (Exception exception) {
        	exception.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            	sqlException.printStackTrace();
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
        }
		
		return jsonArray.toString();
	}
	
	public static String getUserOrder(int userId) {
		
		// Retrieve order records from database and construct JSON object based on results.
		JSONArray jsonArray = new JSONArray();
		Connection connection = null;
        PreparedStatement preparedStatement = null;
		try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            preparedStatement = connection.prepareStatement("SELECT SYMBOL, QUANTITY, PRICE, DATETIME FROM ORDERS WHERE USERS_ID = ? ORDER BY DATETIME DESC");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
    		while (resultSet.next()) {
    			JSONObject jsonObject = new JSONObject();
    			jsonObject.put("type", resultSet.getDouble("QUANTITY") > 0 ? "BUY" : "SELL");
    			jsonObject.put("symbol", resultSet.getString("SYMBOL"));
    			jsonObject.put("quantity", resultSet.getDouble("QUANTITY") >= 0 ? resultSet.getDouble("QUANTITY") : -1 * resultSet.getDouble("QUANTITY"));
    			jsonObject.put("price", resultSet.getDouble("PRICE"));
    			jsonObject.put("datetime", resultSet.getTimestamp("DATETIME"));
    			jsonArray.put(jsonObject);
    		}
    		resultSet.close();
        } catch (Exception exception) {
        	exception.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null)
                    connection.close();
            } catch (SQLException sqlException) {
            	sqlException.printStackTrace();
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException sqlException) {
            }
        }
		
		return jsonArray.toString();
	}
}
