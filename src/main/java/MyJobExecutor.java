import org.apache.log4j.PropertyConfigurator;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Java Quartz Job
 */

public class MyJobExecutor implements Job {

    private static final SimpleDateFormat TIMESTAMP_FMT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSS");
    public static final String EXECUTION_COUNT = "EXECUTION_COUNT";

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {

        JobDataMap jobDataMap = ctx.getJobDetail().getJobDataMap();

        String currentDate = TIMESTAMP_FMT.format(new Date());
        String triggerKey = ctx.getTrigger().getKey().toString();
        String message = jobDataMap.getString("message");

        int executeCount = 0;
        if(jobDataMap.containsKey(EXECUTION_COUNT)) {
            executeCount = jobDataMap.getInt(EXECUTION_COUNT);
        }

        executeCount += 1;
        jobDataMap.put(EXECUTION_COUNT, executeCount);

        System.out.println(String.format("[%-18s][%d][%s] %s", "execute", executeCount, currentDate, message));
    }
}

/**
 * Quartz Scheduler 실행
 */

class JobLauncher {
    public static void main(String[] args) {

        PropertyConfigurator.configure("log4j.properties");

        try {

            // Scheduler 생성
            SchedulerFactory factory = new StdSchedulerFactory();
            Scheduler scheduler = factory.getScheduler();

            // Listener 설정
            ListenerManager listenerManager = scheduler.getListenerManager();
            listenerManager.addJobListener(new MyJobListener());
            listenerManager.addTriggerListener(new MyTriggerListener());

            // Scheduler 실행
            scheduler.start();

            // Job Executor class
            Class<? extends Job> jobClass = MyJobExecutor.class;

            // Job Data 객체 생성
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("message", "Hello, Quartz!!");
            jobDataMap.put(MyJobExecutor.EXECUTION_COUNT, 0);

            // Job 생성
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity("job_name", "job_group")
                    .setJobData(jobDataMap)
                    .build();

            // SimpleTrigger 생성
            // 4초마다 반복, 최대 10회 실행
            SimpleScheduleBuilder simpleSch = SimpleScheduleBuilder.simpleSchedule()
                    .withRepeatCount(10)
                    .withIntervalInMilliseconds(4);

            SimpleTrigger simpleTrigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                    .withIdentity("simple_trigger", "simple_trigger_group")
                    .withSchedule(simpleSch)
                    .forJob(jobDetail)
                    .build();

//            // CronTrigger 생성
//            // 10초 주기로 반복 (0, 10, 20, 30)
//            CronScheduleBuilder cronSch = CronScheduleBuilder.cronSchedule(new CronExpression("0/10 * * * * ?"));
//            CronTrigger cronTrigger = (CronTrigger) TriggerBuilder.newTrigger()
//                    .withIdentity("cron_trigger", "cron_trigger_group")
//                    .forJob(jobDetail)
//                    .build();

            // JobDetail : Trigger = 1 : N 섲렁
//            Set<Trigger> triggerSet = new HashSet<Trigger>();
//            triggerSet.add(simpleTrigger);
//            triggerSet.add(cronTrigger);

            // Schedule 등록
            scheduler.scheduleJob(jobDetail, simpleTrigger);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}

