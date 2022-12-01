package swDBService;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * Servlet implementation class Booking
 */
@WebServlet("/Booking")
public class Booking extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Booking() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request,response);
		//response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//doGet(request, response);
		SWDB mediator = new SWDB();

		if(request.getParameter("reqType").toString().equals("doQuery")){
			String param01 = request.getParameter("param01").toString();
			String param02 = request.getParameter("param02").toString();
			String param03 = request.getParameter("param03").toString();
			String pathToDB = this.getServletContext().getRealPath("/res/BookingDB.ttl");
			mediator.searchForResult(pathToDB, param01, param02, param03);
	    }
		
		PrintWriter out = response.getWriter();
		out.write(mediator.getResult());  
		out.flush();
	    out.close();
		
		
		
		
	}

}
