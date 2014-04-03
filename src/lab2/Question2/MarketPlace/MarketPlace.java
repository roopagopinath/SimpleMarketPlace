package lab2.Question2.MarketPlace;

import javax.jws.WebService;

import java.lang.Exception;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


@WebService
public class MarketPlace {

	private Connection ConnectionMethod(){
		Connection con = null;

		try{

			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/JDBC_LEARN","root","root");

			if(!con.isClosed()){
				return con;
			}
			return con;
		}
		catch(Exception e){
			System.out.println("Connection to DB could not be established");
			return con;
		}

	}


	private int getRowCount(ResultSet resultSet) {
		if (resultSet == null)
		{
			return 0;
		}
		try
		{
			resultSet.last();
			return resultSet.getRow();
		}
		catch (SQLException exp) 
		{
			exp.printStackTrace();
		} 
		finally
		{
			try 
			{
				resultSet.beforeFirst();
			}
			catch (SQLException exp)
			{
				exp.printStackTrace();
			}
		}
		return 0;
	}


	//Method to Sign-up
	public boolean signUp(String first_name, String last_name, String emailID, String password, String userID)
	{
		try{
			Connection con = ConnectionMethod();

			PreparedStatement insStatement = con.prepareStatement("insert into tbl_userinfo values(?,?,?,?,?,?)");
			insStatement.setString(1, first_name);
			insStatement.setString(2, last_name);
			insStatement.setString(3, emailID);
			insStatement.setString(4, password);
			insStatement.setString(5, userID);
			insStatement.setString(6, null);

			insStatement.execute();

			return true;
		}
		catch(Exception ex){
			ex.printStackTrace();
			return false;
		}
	}

	//Method to Sign-in
	public String signIn(String userID, String password)
	{
		try
		{
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			String loginTime = dateFormat.format(date);

			Connection con = ConnectionMethod();

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("select * from tbl_userinfo");

			String loginTime_db = null;
			int flag = 0;
			while(rs.next()){
				String userID_db = rs.getString("userID");
				String passw_db = rs.getString("password1");

				if(userID_db.equals(userID) && passw_db.equals(password)){
					loginTime_db = rs.getString("loginTime");
					
					PreparedStatement delStatement = con.prepareStatement("delete from tbl_shoppingCart where buyerID = ?");
					delStatement.setString(1, userID);

					delStatement.execute();

					PreparedStatement insStatement = con.prepareStatement("update tbl_userinfo set loginTime = ? where userID = ?");
					insStatement.setString(1, loginTime);
					insStatement.setString(2, userID);

					insStatement.execute();

					flag = 1;
					break;
				}

			}

			if(loginTime_db == null){
				loginTime_db = "-1";
			}

			if(flag == 1){
				return "true"+ ":"+ loginTime_db;
			}
			else{
				return "false"+":"+loginTime_db;
			}

		}
		catch(Exception e){
			e.printStackTrace();
			return "false:null";
		}

	}

	//Method to SignOut
	public void signOut(String userID){
		try{
			Connection con = ConnectionMethod();

			PreparedStatement insStatement = con.prepareStatement("delete from tbl_shoppingCart where buyerID = ?");
			insStatement.setString(1, userID);
			boolean st = insStatement.execute();
			System.out.println(st);

		}
		catch(Exception e){
			e.printStackTrace();
		}


	}


	//Method to store Advertisement into DB
	public boolean storeAdvertisement(String itemName, String itemDesc, float price, int quantity, String userID){
		try{

			Connection con = ConnectionMethod();

			PreparedStatement insStatement = con.prepareStatement("insert into tbl_advertisement values(?,?,?,?,?,?)");
			insStatement.setString(1, itemName);
			insStatement.setString(2, itemDesc);
			insStatement.setFloat(3, price);
			insStatement.setInt(4, quantity);
			insStatement.setString(5, userID);
			insStatement.setInt(6, 0);//identity_insert is on for this table. So passing 0
			insStatement.execute();

			return true;

		}
		catch(Exception e){
			e.printStackTrace();
			return false;

		}

	}

	//Method to display advertisements
	public String[] displayAdvertisement(String buyerID){
		try{
			int count = 0;

			Connection con = ConnectionMethod();	
			
			PreparedStatement selStatement = con.prepareStatement("select * from tbl_advertisement where quantity > 0"); 
			ResultSet rs = selStatement.executeQuery();

			count = getRowCount(rs);

			String[] a;
			if(count == 0){
				return null;
			}
			else
			{
				a = new String[count];
				int i = 0;

				for(i = 0; i< count && rs.next(); i++)
				{
					String itemName = rs.getString("item_name");
					String itemDesc = rs.getString("item_desc");
					float price = rs.getFloat("price");
					int quantity = rs.getInt("quantity");
					String userID = rs.getString("userID");
					int itemID = rs.getInt("itemID");

					PreparedStatement insStatement = con.prepareStatement("select quantity from tbl_shoppingCart where itemID = ? and sellerID = ? and buyerID = ?"); 
					insStatement.setInt(1, itemID);
					insStatement.setString(2, userID);
					insStatement.setString(3, buyerID);

					ResultSet rs2 = insStatement.executeQuery();
					int cartcount = getRowCount(rs2);
					
					int cartquantity = 0;
					if(cartcount != 0){
						rs2.next();
						cartquantity = rs2.getInt("quantity");
					}

					quantity -= cartquantity;

					a[i] = userID + ":" + itemName + ":" + itemDesc + ":" + price + ":" + quantity + ":" + itemID;

				}
				return a;
			}

		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}


	//Method to update Shopping cart count
	public boolean updateShoppingCart(String buyerID,String sellerID, int itemID, int quantity, boolean addToCart){
		try{
			Connection con = ConnectionMethod();
			
			System.out.println("Buyer : "+buyerID + " SellerID : "+sellerID + " quantity : "+quantity + " AddToCart : "+addToCart);

			PreparedStatement selStatement = con.prepareStatement("select * from tbl_shoppingCart where buyerID = ? and itemID = ? and sellerID = ?");
			selStatement.setString(1, buyerID);
			selStatement.setInt(2, itemID);
			selStatement.setString(3, sellerID);
			ResultSet rs = selStatement.executeQuery();
			boolean isPresent = false;
			int quantity_db = 0;
			if(rs.next())
			{
				isPresent = true;
				quantity_db = rs.getInt("quantity");
			}

			if(addToCart == true){
				quantity_db+=quantity;
			}
			else {
				quantity_db-=quantity;
			}

			if(isPresent){
				if(quantity_db == 0){
					PreparedStatement insStatement = con.prepareStatement("delete from tbl_shoppingCart where buyerID = ? and itemID = ? and sellerID = ?");
					insStatement.setString(1, buyerID);
					insStatement.setInt(2, itemID);
					insStatement.setString(3, sellerID);
					insStatement.execute();

				}
				else{
					PreparedStatement insStatement = con.prepareStatement("update tbl_shoppingCart set quantity = ? where buyerID = ? and itemID = ? and sellerID = ?");
					insStatement.setInt(1, quantity_db);
					insStatement.setString(2, buyerID);
					insStatement.setInt(3, itemID);
					insStatement.setString(4, sellerID);
					insStatement.execute();
				}

			}
			else{
				PreparedStatement insStatement = con.prepareStatement("insert into tbl_shoppingCart values(?,?,?,?)");
				insStatement.setString(1, buyerID);
				insStatement.setString(2, sellerID);
				insStatement.setInt(3, itemID);
				insStatement.setInt(4, quantity);
				insStatement.execute();

			}


			return true;

		}
		catch(Exception e){
			e.printStackTrace();

			return false;


		}

	}


	//Method to buy Products
	public boolean buyProduct(String buyerID){
		try{
			Connection con = ConnectionMethod();	

			PreparedStatement insStatement = con.prepareStatement("select s.sellerID, s.itemID, s.quantity, a.quantity as AdQuantity from tbl_shoppingCart s, tbl_advertisement a where a.itemID = s.itemID and buyerID = ?");
			insStatement.setString(1, buyerID);
			ResultSet rs = insStatement.executeQuery();

			int count = getRowCount(rs);
			boolean stat = true;
			System.out.println("Count in shopping cart " + count);
			if(count != 0)
			{
				stat = false;
				while(rs.next() && !stat){
					int quantity = rs.getInt("quantity");
					int itemID = rs.getInt("itemID");
					String sellerID = rs.getString("sellerID");
					int AdQuantity = rs.getInt("AdQuantity");
					if(AdQuantity < quantity){
						PreparedStatement delStatement = con.prepareStatement("delete from tbl_shoppingCart where buyerID = ?"); 
						delStatement.setString(1, buyerID);
						delStatement.execute();
						
						return false;
					}
					
					System.out.println("quantity "+ quantity + "item ID "+ itemID + "sellerID "+ sellerID);

					//updating advertisement table
					PreparedStatement updStatement = con.prepareStatement("update tbl_advertisement set quantity = (quantity - ?) where itemID = ?"); 
					updStatement.setInt(1, quantity);
					updStatement.setInt(2, itemID);
					stat = updStatement.execute();
					System.out.println("Update advertisement table " + stat);

					if(!stat){
						insStatement = con.prepareStatement("insert into tbl_transaction values(?,?,?,?,?)"); 
						insStatement.setString(1, buyerID);
						insStatement.setString(2, sellerID);
						insStatement.setInt(3, itemID);
						insStatement.setInt(4, quantity);

						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date date = new Date();
						String transactionTime = dateFormat.format(date);

						insStatement.setString(5, transactionTime);

						stat = insStatement.execute();
						System.out.println("insert into transaction table "+ stat);
						
						if(!stat){
							PreparedStatement delStatement = con.prepareStatement("delete from tbl_shoppingCart where buyerID = ? and quantity = ? and itemID = ?");
							delStatement.setString(1, buyerID);
							delStatement.setInt(2, quantity);
							delStatement.setInt(3, itemID);
							
							stat = delStatement.execute();
						}
						
					}

				}
			}
			return !stat;
			}
		
		catch(Exception e){
			e.printStackTrace();
			return false;
		}

	}

	public String[] buy(String buyerID){
		try{
			Connection con = ConnectionMethod();
			PreparedStatement selStatement = con.prepareStatement("select * from tbl_transaction t, tbl_advertisement a where buyerID = ? and a.itemID = t.itemID ");
			selStatement.setString(1, buyerID);

			ResultSet rs = selStatement.executeQuery();

			int count = getRowCount(rs);

			String[] bought;
			if(count == 0){
				return null;
			}
			else
			{
				bought = new String[count]; 
				for(int i =0; i < count && rs.next(); i++){
					
					String sellerID = rs.getString("sellerID");
					int quantity = rs.getInt("quantity");
					String transactionTime = rs.getString("transactionTime");
					float price = rs.getFloat("price");
					String item_name = rs.getString("item_name");
					String item_desc = rs.getString("item_desc");

					bought[i] = sellerID + ":" + item_name + ":" + item_desc + ":" + quantity + ":" + price + ":" + transactionTime;
				}
				return bought;
			}

		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}

	}


	public String[] sell(String sellerID){
		try{
			Connection con = ConnectionMethod();
			PreparedStatement selStatement = con.prepareStatement("select * from tbl_transaction t, tbl_advertisement a where sellerID = ? and a.itemID = t.itemID ");
			selStatement.setString(1, sellerID);

			ResultSet rs = selStatement.executeQuery();

			int count = getRowCount(rs);

			if(count == 0){
				return null;
			}
			else
			{

				String[] sold = new String[count]; 

				for(int i =0; i < count && rs.next(); i++){
					
					String buyerID = rs.getString("buyerID");
					int quantity = rs.getInt("quantity");
					String transactionTime = rs.getString("transactionTime");
					float price = rs.getFloat("price");
					String item_name = rs.getString("item_name");
					String item_desc = rs.getString("item_desc");

					sold[i] = buyerID + ":" + item_name + ":" + item_desc + ":" + quantity + ":" + price + ":" + transactionTime;
					
				}
				return sold;
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return null;

		}

	}



	public String[] viewCart(String buyerID){
		try{
			Connection con = ConnectionMethod();	
			//Statement s = con.createStatement();

			//Here, userID and item_name are with respect to the one who has advertised the item. 
			//So shopping cart field in DB is actually is letting us know whose item is getting added to cart 
			PreparedStatement selStatement = con.prepareStatement("select * from tbl_shoppingCart where buyerID = ?");
			selStatement.setString(1, buyerID);

			ResultSet rs = selStatement.executeQuery();
			int count = getRowCount(rs);

			String a[];  

			if(count == 0){
				return null;

			}
			else{

				a = new String[count];
				for(int i =0; i < count && rs.next(); i++){
					int itemID = rs.getInt("itemID");
					int quantity = rs.getInt("quantity");
					String sellerID = rs.getString("sellerID");
					selStatement = con.prepareStatement("select item_name, item_desc, price from tbl_advertisement where itemID = ?");
					selStatement.setInt(1, itemID);
					ResultSet rs2 = selStatement.executeQuery();
					
					rs2.next();
					String itemName = rs2.getString("item_name");
					String itemDesc = rs2.getString("item_desc");
					float price = rs2.getFloat("price");

					a[i] = itemName + ":" + itemDesc + ":" + price + ":" + sellerID + ":" + quantity + ":" + itemID;

				}
			}

			return a;

		}
		catch(Exception e){
			e.printStackTrace();

			return null;


		}

	}

}
