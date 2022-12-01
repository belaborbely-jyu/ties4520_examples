package sswapServiceMediator;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sswapServiceMediator.SSWAPMed;

/**
 * Servlet implementation class SSWAPServiceMed
 */
@WebServlet("/SSWAPServiceMed")
public class SSWAPServiceMed extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SSWAPServiceMed() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request,response);
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//doGet(request, response);
		SSWAPMed mediator = new SSWAPMed();
		if(request.getParameter("reqType").toString().equals("doQuery")){
			String endPoint = request.getParameter("serviceURL").toString();
			mediator.sendRequest(endPoint);
	    }
		
		PrintWriter out = response.getWriter();
		out.write(mediator.getResult());  
		out.flush();
	    out.close();
		
		
		
		
		
		
	}

}
