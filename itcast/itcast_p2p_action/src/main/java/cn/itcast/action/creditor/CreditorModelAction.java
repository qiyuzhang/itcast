package cn.itcast.action.creditor;

import cn.itcast.action.common.BaseAction;
import cn.itcast.domain.creditor.CreditorModel;
import cn.itcast.service.creditor.CreditorModelService;
import cn.itcast.util.constant.ClaimsType;
import cn.itcast.utils.*;
import cn.itcast.utils.excelUtil.*;
import com.opensymphony.xwork2.ModelDriven;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.InterceptorRefs;
import org.apache.struts2.convention.annotation.Namespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@Scope("prototype")
@Namespace("/creditor")
@InterceptorRefs(@InterceptorRef("fileUploadStack"))
public class CreditorModelAction extends BaseAction implements ModelDriven<CreditorModel> {

    private CreditorModel creditor = new CreditorModel();
    private File file;//上传的文件
    private String fileFileName;//上传的文件名称

    @Autowired
    CreditorModelService creditorModelService;

    @Action("addCreditor")
    public void addCreditor() throws IOException {

        creditor.setDebtNo("ZQNO" + RandomNumberUtil.randomNumber(new Date()));//债权id
        creditor.setBorrowerId(1);//贷款人id
        creditor.setDebtStatus(ClaimsType.UNCHECKDE); //债权状态
        creditor.setMatchedMoney(0.00);//匹配金额
        creditor.setMatchedStatus(ClaimsType.UNMATCH); //匹配状态
        creditor.setDebtType(FrontStatusConstants.NULL_SELECT_OUTACCOUNT);//标的类型
        creditor.setAvailablePeriod(creditor.getDebtTransferredPeriod());//可用期限
        creditor.setAvailableMoney(creditor.getDebtTransferredMoney());//可用金额
        creditor.setDebtMonthRate(new DataFormatUtilInterfaceImpl().formatToDouble(creditor.getDebtMonthRate(),12));

        System.out.println("creditorModel = " + creditor);

        creditorModelService.addCreditor(creditor);
        this.getResponse().getWriter().write(Response.build().setStatus("1").toJSON());
    }


    @Action("download")
    public void download() throws Exception {

        //读取资源
        //得到文件位置
        String path = this.getRequest().getSession().getServletContext().getRealPath("WEB-INF/excelTemplate/ClaimsBatchImportTemplate.xlsx");

        //设置下载的两个响应头
        String mimeType = this.getRequest().getSession().getServletContext().getMimeType("ClaimsBatchImportTemplate.xlsx");
        this.getResponse().setHeader("content-type", mimeType);
        this.getResponse().setHeader("content-disposition", "attachment;filename=" + (new Date().toLocaleString() + ".xlsx"));

        FileInputStream fis = new FileInputStream(path);

        //写出资源  response 获取流
        OutputStream outputStream = this.getResponse().getOutputStream();
        IOUtils.copy(fis,outputStream);
    }

//    @Action("upload")
//    public void upload() throws Exception {
//        // 1.完成文件上传操作
//        String path = this.getRequest().getSession().getServletContext().getRealPath("/WEB-INF/upload");
//        File f1 = new File(path);
//
//        String fileName = System.currentTimeMillis()+fileFileName;
//        File f2 = new File(f1,fileName);
//
//        FileUtils.copyFile(file,f2);
//
//        // 1.获取的文件是一个excel，应该将它的内容读取出来，封装成List<Creditor>
//        ExceiUtils<CreditorModel> er = new ExceiUtils<>();
//        List<CreditorModel> list = er.getEntity(f2, new ExcelRowResultHandler<CreditorModel>() {
//            @Override
//            public CreditorModel invoke(List<Object> list) {
//                return null;
//            }
//        });
//
//        //调用 service 完成 save
//        creditorModelService.save(list);
//        System.out.println("list = " + list);
//        System.out.println("上传成功");
//        this.getResponse().getWriter().write(Response.build().setStatus("1").toJSON());
//
//    }

    @Action("upload")
    public void upload() {
        // 1.完成文件上传操作
        // 上传文件保存到WEB-INF/upload下
        String path = this.getRequest().getSession().getServletContext().getRealPath("/WEB-INF/upload");
        String destName = new Date().getTime() + fileFileName;
        File destFile = new File(path, destName); // 目标文件
        InputStream is = null;
        try {
            FileUtils.copyFile(file, destFile);// 文件上传操作
            // 2.将文件中内容读取到封装成List<CreditorModel>
            is = new FileInputStream(path + "/" + destName);
            SimpleExcelUtil<CreditorModel> seu = new SimpleExcelUtil<CreditorModel>();

            List<CreditorModel> cms = seu.getDataFromExcle(is, "", 1, new MatchupData<CreditorModel>() {
                @Override
                public <T> T macthData(List<Object> data, int indexOfRow, DataFormatUtilInterface formatUtil) {
                    CreditorModel creditor = new CreditorModel();
                    if (data.get(0) != null) {
                        creditor.setContractNo(data.get(0).toString()); // 债权合同编号
                    } else {
                        throw new ExcelDataFormatException("{" + 0 + "}");
                    }
                    creditor.setDebtorsName(data.get(1).toString().replaceAll("\\s{1,}", " "));// 债务人名称
                    // 身份证号
                    if (data.get(2) != null) {
                        String data2 = data.get(2).toString().replaceAll("\\s{1,}", " ");
                        String[] art = data2.split(" ");
                        for (int i = 0; i < art.length; i++) {
                            String str = art[i];
                            // if (!RegValidationUtil.validateIdCard(str)) {
                            // throw new ExcelDataFormatException("{" + 2 +
                            // "}");
                            // }
                        }
                        creditor.setDebtorsId(data2);// 债务人身份证编号
                    } else {
                        throw new ExcelDataFormatException("{" + 2 + "}");
                    }
                    creditor.setLoanPurpose(data.get(3).toString()); // 借款用途
                    creditor.setLoanType(data.get(4).toString());// 借款类型
                    creditor.setLoanPeriod(formatUtil.formatToInt(data.get(5), 5)); // 原始期限月
                    creditor.setLoanStartDate(formatUtil.format(data.get(6), 6));// 原始借款开始日期
                    creditor.setLoanEndDate(formatUtil.format(data.get(7), 7));// 原始贷款到期日期
                    // 还款方式
                    if (ConstantUtil.EqualInstallmentsOfPrincipalAndInterest.equals(data.get(8))) {// 等额本息
                        creditor.setRepaymentStyle(11601);
                    } else if (ConstantUtil.MonthlyInterestAndPrincipalMaturity.equals(data.get(8))) {// 按月付息到月还本
                        creditor.setRepaymentStyle(11602);
                    } else if (ConstantUtil.ExpirationTimeRepayment.equals(data.get(8))) {// 到期一次性还款
                        creditor.setRepaymentStyle(11603);
                    } else {
                        throw new ExcelDataFormatException("在单元格{" + 8 + "}类型不存在");
                    }
                    creditor.setRepaymenDate(data.get(9).toString());// 每月还款日
                    creditor.setRepaymenMoney(formatUtil.formatToDouble(data.get(10), 10));// 月还款金额
                    creditor.setDebtMoney(formatUtil.formatToDouble(data.get(11), 11));// 债权金额
                    creditor.setDebtMonthRate(formatUtil.formatToDouble(data.get(12), 12));// 债权月利率
                    creditor.setDebtTransferredMoney(formatUtil.formatToDouble(data.get(13), 13));// 债权转入金额
                    creditor.setDebtTransferredPeriod(formatUtil.formatToInt(data.get(14), 14));// 债权转入期限
                    creditor.setDebtRansferOutDate(formatUtil.format(data.get(15), 15));// 债权转出日期
                    creditor.setCreditor(data.get(16).toString());// 债权人

                    // 债权转入日期 原始开始日期+期限
                    Date startDate = formatUtil.format(data.get(6), 6); // 原始开始日期
                    int add = formatUtil.formatToInt(data.get(14), 14);// 期限
                    Calendar c = Calendar.getInstance();
                    c.setTime(startDate);
                    c.add(Calendar.MONTH, add);
                    creditor.setDebtTransferredDate(c.getTime());

                    Date da = new Date();
                    creditor.setDebtNo("ZQNO" + RandomNumberUtil.randomNumber(da));// 债权编号
                    creditor.setMatchedMoney(Double.valueOf("0"));// 已匹配金额
                    creditor.setDebtStatus(ClaimsType.UNCHECKDE); // 债权状态
                    creditor.setMatchedStatus(ClaimsType.UNMATCH);// 债权匹配状态
                    creditor.setBorrowerId(1); // 借款人id
                    creditor.setDebtType(FrontStatusConstants.NULL_SELECT_OUTACCOUNT); // 标的类型
                    creditor.setAvailablePeriod(creditor.getDebtTransferredPeriod());// 可用期限
                    creditor.setAvailableMoney(creditor.getDebtTransferredMoney());// 可用金额
                    return (T) creditor;

                }
            });
            // 3.调用service完成批量导入操作
            creditorModelService.save(cms);
            this.getResponse().getWriter().write(Response.build().setStatus("1").toJSON());
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            this.getResponse().getWriter().write(Response.build().setStatus("0").toJSON());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileFileName() {
        return fileFileName;
    }

    public void setFileFileName(String fileFileName) {
        this.fileFileName = fileFileName;
    }

    @Override
    public CreditorModel getModel() {
        return creditor;
    }
}
