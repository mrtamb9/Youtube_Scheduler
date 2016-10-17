package youtube.automation;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import parameter.Parameters;

public class scheduler {
	static String username = "";
	static String password = "";
	static String public_date = "";
	static String public_hour = "";
	static int batch_size = 3;
	static int number_new_videos = 0;
	static String file_thumbnail = "";
	static int number_thumbnails = 0;
	static int from_thumbnail = 1;
	
	static String start_id = "";
	
	static String status_saved = "All changes saved.";
	static String status_not_saved = "Some changes are not yet saved.";

	static WebDriver driver;
	static WebDriverWait wait;
	static HashMap<String, Integer> mapHours = new HashMap<>();
	
	static int number_edited_videos = 0;

	private static void init() throws IOException, ParseException, InterruptedException, AWTException {
		System.out.println("=== Author: tamnt ===");
		System.out.println("Start init ....");

		// read list hours
		String line;
		BufferedReader br_hour = new BufferedReader(new InputStreamReader(new FileInputStream(Parameters.file_hours)));
		int index = 0;
		while ((line = br_hour.readLine()) != null) {
			mapHours.put(line, index);
			index++;
		}
		br_hour.close();

		// identification
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(Parameters.file_info)));
		while ((line = br.readLine()) != null) {
			String[] tempArray = line.split("=");
			if (tempArray[0].compareToIgnoreCase("username") == 0) {
				username = tempArray[1];
				System.out.println("username = " + username);
			} else if (tempArray[0].compareToIgnoreCase("password") == 0) {
				password = tempArray[1];
			} else if (tempArray[0].compareToIgnoreCase("public_date_start") == 0) {
				public_date = tempArray[1];
				System.out.println("public_date_start = " + public_date);
			} else if (tempArray[0].compareToIgnoreCase("public_hour") == 0) {
				public_hour = tempArray[1];
				if (!mapHours.containsKey(public_hour)) {
					System.out.println("Not contain " + public_hour + " in file hours.txt");
					System.exit(1);
				}
				System.out.println("public_hour = " + public_hour);
			} else if (tempArray[0].compareToIgnoreCase("new_upload_videos") == 0) {
				number_new_videos = Integer.parseInt(tempArray[1]);
				System.out.println("new_upload_videos = " + number_new_videos);
			} else if (tempArray[0].compareToIgnoreCase("videos_public_per_day") == 0) {
				batch_size = Integer.parseInt(tempArray[1]);
				System.out.println("videos_public_per_day = " + batch_size);
			} else if (tempArray[0].compareToIgnoreCase("output_date_format")==0)
			{
				String dateFormat = tempArray[1];
				if(dateFormat.compareTo("dd-MM-yyyy")==0 || dateFormat.compareTo("MMM dd, yyyy")==0)
				{
					Parameters.dfoutput = new SimpleDateFormat(dateFormat);
				} else {
					System.out.println("Wrong date format: use dd-MM-yyyy or MMM dd, yyyy");
					System.exit(1);
				}
				System.out.println("output_date_format = " + dateFormat);
			} else if (tempArray[0].compareToIgnoreCase("file_thumbnail") == 0) {
				file_thumbnail = tempArray[1];
				System.out.println("folder_thumbnail = " + file_thumbnail);
			} else if (tempArray[0].compareToIgnoreCase("number_thumbnails") == 0) {
				number_thumbnails = Integer.parseInt(tempArray[1]);
				System.out.println("number_thumbnails = " + number_thumbnails);
			} else if (tempArray[0].compareToIgnoreCase("from_thumbnail") == 0) {
				from_thumbnail = Integer.parseInt(tempArray[1]);
				System.out.println("from_thumbnail = " + from_thumbnail);
			} else if(tempArray[0].compareToIgnoreCase("file_driver")==0)
			{
				Parameters.file_driver = tempArray[1];
			} else if(tempArray[0].compareToIgnoreCase("start_id")==0)
			{
				start_id = tempArray[1].trim();
				System.out.println(start_id);
			}
		}
		br.close();
		
		Date tempdate = Parameters.dfinput.parse(public_date);
		public_date = Parameters.dfoutput.format(tempdate);

		System.setProperty("webdriver.chrome.driver", Parameters.file_driver);
		
//		FirefoxProfile profile = new FirefoxProfile();
//		profile.setAcceptUntrustedCertificates(true); 
//		profile.setAssumeUntrustedCertificateIssuer(false);
//		driver = new FirefoxDriver(profile);
		
		driver = new ChromeDriver();
		
		driver.manage().timeouts().implicitlyWait(100, TimeUnit.SECONDS);
		wait = new WebDriverWait(driver, 100);

		// login
		driver.get("https://accounts.google.com/ServiceLogin?passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26feature%3Dsign_in_button%26next%3D%252F%26hl%3Den&service=youtube&uilel=3&hl=en#identifier");
		driver.findElement(By.id("Email")).sendKeys(username);
		driver.findElement(By.id("next")).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("Passwd")));

		driver.findElement(By.id("Passwd")).sendKeys(password);
		driver.findElement(By.id("signIn")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("masthead-positioner")));
		
		System.out.println("Done init!");
	}

	private static void scheduleTime(String id) throws InterruptedException, AWTException 
	{
		driver.get("https://www.youtube.com/edit?o=U&video_id=" + id);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//form[@class=\"video-settings-form ng-valid ng-valid-parse ng-valid-maxlength ng-pristine\"]")));

		if(number_edited_videos == 0)
		{
			// change language --> English (in case it was not in English)
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("yt-picker-language-button")));
			Thread.sleep(1000);
			driver.findElement(By.id("yt-picker-language-button")).click();
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//strong[@class=\"yt-picker-item\"]")));
			System.out.println(driver.findElement(By.xpath("//strong[@class=\"yt-picker-item\"]")).getText());
			Thread.sleep(1000);
			if(driver.findElement(By.xpath("//strong[@class=\"yt-picker-item\"]")).getText().compareTo("English (US)")!=0)
			{
				driver.findElement(By.xpath("//button[@value=\"en\"]")).click();
			}
			// wait until change done
			while(true)
			{
				System.out.println("Wait until change to English done....");
				if(driver.findElement(By.xpath("//link[@rel=\"search\"]")).getAttribute("href").toString().contains("locale=en_US"))
				{
					break;
				}
				Thread.sleep(500);
			}
		}
		
		if(driver.findElement(By.xpath("//select[@class=\"yt-uix-form-input-select-element metadata-privacy-input\"]")).getAttribute("data-initial-value").toString().compareTo("public")==0)
		{
			System.out.println("Status video: public");
		} else 
		{
			if(driver.findElement(By.xpath("//select[@class=\"yt-uix-form-input-select-element metadata-privacy-input\"]")).getAttribute("data-initial-value").toString().compareTo("scheduled")==0)
			{
				System.out.println("Status video: scheduled");
			} else {
				System.out.println("Status video: not scheduled");
				
				// change status video to "private"
				for (int i = 0; i < 4; i++) {
					driver.findElement(By.xpath("//select[@class=\"yt-uix-form-input-select-element metadata-privacy-input\"]"))
							.sendKeys(Keys.ARROW_UP);
				}
				for (int i = 0; i < 2; i++) {
					driver.findElement(By.xpath("//select[@class=\"yt-uix-form-input-select-element metadata-privacy-input\"]"))
							.sendKeys(Keys.ARROW_DOWN);
				}
				
				// save change status video
				driver.findElement(By
						.xpath("//button[@class=\"yt-uix-button yt-uix-button-size-default save-changes-button yt-uix-tooltip yt-uix-button-primary\"]"))
						.click();
				Thread.sleep(500);
				
				// wait until save done
				while(true)
				{
					System.out.println(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText());
					if(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText().contains(status_saved))
					{
						break;
					}
					Thread.sleep(500);
				}

				// change status video to scheduler
				driver.findElement(By.xpath("//select[@class=\"yt-uix-form-input-select-element metadata-privacy-input\"]"))
					.sendKeys(Keys.ARROW_DOWN);
				
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@class=\"yt-uix-form-input-text publish-date-formatted\"]")));
			}
			
			// set date
			driver.findElement(By.xpath("//input[@class=\"yt-uix-form-input-text publish-date-formatted\"]")).clear();
			driver.findElement(By.xpath("//input[@class=\"yt-uix-form-input-text publish-date-formatted\"]"))
					.sendKeys(public_date);

			// set hour
			for (int i = 0; i < mapHours.size(); i++) {
				driver.findElement(By.xpath("//select[@class=\"yt-uix-form-input-select-element publish-time-formatted\"]"))
						.sendKeys(Keys.ARROW_UP);
			}
			int index_hour = mapHours.get(public_hour);
			for (int i = 0; i < index_hour; i++) {
				driver.findElement(By.xpath("//select[@class=\"yt-uix-form-input-select-element publish-time-formatted\"]"))
						.sendKeys(Keys.ARROW_DOWN);
			}
			
			// submit
			driver.findElement(By
					.xpath("//button[@class=\"yt-uix-button yt-uix-button-size-default save-changes-button yt-uix-tooltip yt-uix-button-primary\"]"))
					.click();
			
			while(true)
			{
				System.out.println(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText());
				if(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText().contains(status_saved))
				{
					break;
				}
				Thread.sleep(500);
			}
			
			// set thumbnail
			if(driver.getPageSource().contains("selectable-thumb custom-thumb hid small-thumb-dimensions"))
			{
				Thread.sleep(1000);
				
				// firefox
//				driver.findElement(By.
//						xpath("//button[@class=\"yt-uix-button yt-uix-button-size-default yt-uix-button-default custom-thumb-button\"]"))
//						.click();
//				
				// chrome
				driver.findElement(By.
						xpath("//div[@class=\"custom-thumb-container\"]"))
						.click();
				
				Thread.sleep(1000);
				System.out.println(file_thumbnail.replace("index", from_thumbnail + ""));
				
				StringSelection ss = new StringSelection(file_thumbnail.replace("index", from_thumbnail + ""));
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
				
				Robot robot = new Robot();
				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);
				Thread.sleep(500);
				robot.keyPress(KeyEvent.VK_CONTROL);
				robot.keyPress(KeyEvent.VK_V);
				robot.keyRelease(KeyEvent.VK_V);
				robot.keyRelease(KeyEvent.VK_CONTROL);
				Thread.sleep(500);
				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);
				
				while(true)
				{
					System.out.println(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText());
					if(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText().contains(status_not_saved))
					{
						break;
					}
					Thread.sleep(500);
				}
				
				// submit
				driver.findElement(By
						.xpath("//button[@class=\"yt-uix-button yt-uix-button-size-default save-changes-button yt-uix-tooltip yt-uix-button-primary\"]"))
						.click();
			}
			
			while(true)
			{
				System.out.println(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText());
				if(driver.findElement(By.xpath("//span[@class=\"save-error-message\"]")).getText().contains(status_saved))
				{
					break;
				}
				Thread.sleep(500);
			}
			
			System.out.println("5");
		}
		number_edited_videos++;
	}

	private static String increaseDate(String currentDateString) throws ParseException {
		Calendar c = Calendar.getInstance();
		c.setTime(Parameters.dfoutput.parse(currentDateString));
		c.add(Calendar.DATE, 1); // number of days to add
		return Parameters.dfoutput.format(c.getTime());
	}

	private static ArrayList<String> getListNewVideos() 
	{
		ArrayList<String> listNewVideos = new ArrayList<>();
		
		int count_video = 0;
		int page_number = 0;
		while (count_video < number_new_videos) 
		{
			page_number++;
			driver.get("https://www.youtube.com/my_videos?o=U&pi=" + page_number);
			wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//a[@class=\"vm-video-title-content yt-uix-sessionlink\"]")));
			List<WebElement> listElementVideos = driver
					.findElements(By.xpath("//a[@class=\"vm-video-title-content yt-uix-sessionlink\"]"));
			
			for (WebElement video : listElementVideos) 
			{
				String link = video.getAttribute("href");
				System.out.println(link);
				listNewVideos.add(link);
				count_video++;
				if (count_video >= number_new_videos) {
					break;
				}
			}
		}

		return listNewVideos;
	}
	
	private static void saveLinks(ArrayList<String> listLinks) throws IOException
	{
		FileWriter fw = new FileWriter(Parameters.file_link);
		for (String link : listLinks) {
			fw.write(link + "\n");
		}
		fw.close();
	}

	public static void main(String[] args) throws IOException, ParseException, InterruptedException, AWTException 
	{
		init();

		ArrayList<String> listLinks = getListNewVideos();
		// saveLinks(listLinks);
		
		int start_index = 0;
		if(start_id.length() > 0)
		{
			while(start_index < listLinks.size())
			{
				if(listLinks.get(start_index).contains(start_id))
				{
					break;
				}
				start_index++;
			}
		}

		System.out.println("start_index = " + start_index);
		int origin_from_thumbnail = from_thumbnail;
		for (int i = start_index; i < listLinks.size(); i++) 
		{
			System.out.println("Link " + (i + 1));
			String id = listLinks.get(i).replace("https://www.youtube.com/watch?v=", "");
			System.out.println(public_date);
			try {
				scheduleTime(id);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if ((i + 1) % batch_size == 0) {
				public_date = increaseDate(public_date);
			}
			
			from_thumbnail++;
			if(from_thumbnail > number_thumbnails)
			{
				from_thumbnail = origin_from_thumbnail;
			}
		}

		driver.get("https://www.youtube.com/my_videos?o=U");
		Thread.sleep(1000);
		driver.close();
		System.out.println("All done!");
	}
}