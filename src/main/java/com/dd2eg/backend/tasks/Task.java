package com.dd2eg.backend.tasks;

import com.dd2eg.backend.tasks.comments.Comment;
import com.dd2eg.backend.tasks.commits.Commit;
import com.dd2eg.backend.tasks.sponsorship.SponsorshipDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Document(collection = "tasks")
@NoArgsConstructor
@Sharded(shardKey = { "projectId", "_id" })
@CompoundIndex(name = "project_status_idx", def = "{'projectId': 1, 'status': 1}")
public class Task {

    @Id
    private String id;

    private String title;

    private String description;

    private String body;

    @Indexed(partialFilter = "{ status: 'OPEN' }")
    private TaskStatus status = TaskStatus.PENDING;

    private String priority;

    private Integer numMaxCommits;

    private String projectId;

    private Integer budget;

    @Transient
    private int currentIndex = 0;

    // we save name of the enterprise and the amount
    // amount < budget
    private List<SponsorshipDTO> sponsorships;

    private List<String> skills;

    //pre-allocation of commits of numMaxCommits
    private List<Commit> commits;

    private List<Comment> comments = new ArrayList<>();

    public Task(int numMaxCommits) {
        this.commits = new ArrayList<>(numMaxCommits);
        for (int i = 0; i < numMaxCommits; i++) {
            this.commits.add(new Commit());
        }
    }

    public int getCurrentIndex() {
        if (commits == null) return 0;

        for (int i = 0; i < commits.size(); i++) {

            if (commits.get(i).getHash() == null) {
                return i;
            }
        }

        return commits.size();
    }
}
