package com.workload.system.domain.dto;

import java.math.BigDecimal;

/**
 * 附件1「一行一开课任务」数据行（对应模板第 5 行起，A~AM 共 39 列）。
 * <p>
 * 行粒度（2026-09-10 用户确认）：按开课任务聚合——同一 task_id 的 G1~G6 明细合为一行，
 * 无对应环节的列留空；教师级列（学院/姓名/G7~AM）不在此 DTO，由首行另行填充。
 * 手工/申报明细无 task_id，按 item_id 单独成行。
 */
public class Attachment1RowDTO
{
    /** C 课程名称（task 优先，手工明细取 item 快照） */
    private String courseName;

    /** D 层次：本科（含专升本归一）/专科 */
    private String educationLevel;

    /** E 理论课工作量 J1 */
    private BigDecimal j1;

    /** F 是否重复课 C1 */
    private BigDecimal c1;

    /** G 课程类型 K1 */
    private BigDecimal k1;

    /** H 教学质量系数 Q1 */
    private BigDecimal q1;

    /** I 课程质量系数 Q2 */
    private BigDecimal q2;

    /** J 非语言类全外文课程系数 Q3 */
    private BigDecimal q3;

    /** K 合堂系数 N */
    private BigDecimal n;

    /** L G1 = J1*C1*K1*Q1*Q2*N */
    private BigDecimal g1;

    /** M 实践课学时数 J2 */
    private BigDecimal j2;

    /** N 实践课课程系数 K */
    private BigDecimal practiceK;

    /** O 实践课重复系数 C2 */
    private BigDecimal c2;

    /** P G2 = J2*K*C2*Q1*Q2 */
    private BigDecimal g2;

    /** Q 教学实际天数 T */
    private BigDecimal t;

    /** R 实习实训系数 D */
    private BigDecimal d;

    /** S 重复系数 K */
    private BigDecimal internK;

    /** T G3 = T*D*K*Q1*Q2 */
    private BigDecimal g3;

    /** U 课程设计学分 J4 */
    private BigDecimal j4;

    /** V 课程设计人数 R4 */
    private BigDecimal r4;

    /** W G4 = J4*R4*0.4 */
    private BigDecimal g4;

    /** X 指导毕业论文（设计）G5 文科列 */
    private BigDecimal g5Liberal;

    /** Y 指导毕业论文（设计）G5 理工科列 */
    private BigDecimal g5Scitech;

    /** Z 实习周数 W */
    private BigDecimal w;

    /** AA 学生人数 R6 */
    private BigDecimal r6;

    /** AB G6 = W*R6*0.4 */
    private BigDecimal g6;

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public BigDecimal getJ1() { return j1; }
    public void setJ1(BigDecimal j1) { this.j1 = j1; }
    public BigDecimal getC1() { return c1; }
    public void setC1(BigDecimal c1) { this.c1 = c1; }
    public BigDecimal getK1() { return k1; }
    public void setK1(BigDecimal k1) { this.k1 = k1; }
    public BigDecimal getQ1() { return q1; }
    public void setQ1(BigDecimal q1) { this.q1 = q1; }
    public BigDecimal getQ2() { return q2; }
    public void setQ2(BigDecimal q2) { this.q2 = q2; }
    public BigDecimal getQ3() { return q3; }
    public void setQ3(BigDecimal q3) { this.q3 = q3; }
    public BigDecimal getN() { return n; }
    public void setN(BigDecimal n) { this.n = n; }
    public BigDecimal getG1() { return g1; }
    public void setG1(BigDecimal g1) { this.g1 = g1; }
    public BigDecimal getJ2() { return j2; }
    public void setJ2(BigDecimal j2) { this.j2 = j2; }
    public BigDecimal getPracticeK() { return practiceK; }
    public void setPracticeK(BigDecimal practiceK) { this.practiceK = practiceK; }
    public BigDecimal getC2() { return c2; }
    public void setC2(BigDecimal c2) { this.c2 = c2; }
    public BigDecimal getG2() { return g2; }
    public void setG2(BigDecimal g2) { this.g2 = g2; }
    public BigDecimal getT() { return t; }
    public void setT(BigDecimal t) { this.t = t; }
    public BigDecimal getD() { return d; }
    public void setD(BigDecimal d) { this.d = d; }
    public BigDecimal getInternK() { return internK; }
    public void setInternK(BigDecimal internK) { this.internK = internK; }
    public BigDecimal getG3() { return g3; }
    public void setG3(BigDecimal g3) { this.g3 = g3; }
    public BigDecimal getJ4() { return j4; }
    public void setJ4(BigDecimal j4) { this.j4 = j4; }
    public BigDecimal getR4() { return r4; }
    public void setR4(BigDecimal r4) { this.r4 = r4; }
    public BigDecimal getG4() { return g4; }
    public void setG4(BigDecimal g4) { this.g4 = g4; }
    public BigDecimal getG5Liberal() { return g5Liberal; }
    public void setG5Liberal(BigDecimal g5Liberal) { this.g5Liberal = g5Liberal; }
    public BigDecimal getG5Scitech() { return g5Scitech; }
    public void setG5Scitech(BigDecimal g5Scitech) { this.g5Scitech = g5Scitech; }
    public BigDecimal getW() { return w; }
    public void setW(BigDecimal w) { this.w = w; }
    public BigDecimal getR6() { return r6; }
    public void setR6(BigDecimal r6) { this.r6 = r6; }
    public BigDecimal getG6() { return g6; }
    public void setG6(BigDecimal g6) { this.g6 = g6; }
}
