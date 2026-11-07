package com.workload.system.review;

import java.util.*;
import org.springframework.stereotype.Service;

/** Deliberately no parent transaction: every item calls a separate transactional bean. */
@Service
public class ReviewBatchCommandService {
    private final ItemReviewService items;
    public ReviewBatchCommandService(ItemReviewService items){this.items=items;}
    public Map<String,Object> review(List<ReviewCommands.Entry> commands,String action){
        ItemReviewService.authorize(action);
        if(commands==null||commands.isEmpty()||commands.size()>100||commands.stream().map(ReviewCommands.Entry::id).distinct().count()!=commands.size())throw ReviewCommands.bad("批量必须1至100项且不重复");
        int success=0;List<Map<String,Object>> failures=new ArrayList<>();
        for(var c:commands)try{items.review(c.id(),action,c.command());success++;}catch(RuntimeException e){failures.add(Map.of("id",c.id(),"reason",String.valueOf(e.getMessage())));}
        return Map.of("successCount",success,"failCount",failures.size(),"failDetails",failures);
    }
}
