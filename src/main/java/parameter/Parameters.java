package parameter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Parameters {
	public static String file_link = "links.txt";
	public static String file_info = "info.txt";
	public static String file_hours = "hours.txt";
	public static String file_driver = "geckodriver/chromedriver.exe";

	public static DateFormat dfinput = new SimpleDateFormat("dd-MM-yyyy");
	public static DateFormat dfoutput = new SimpleDateFormat("MMM dd, yyyy");
}
