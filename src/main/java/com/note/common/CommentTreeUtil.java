package com.note.common;

import com.note.interaction.entity.Comment;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CommentTreeUtil {

    private CommentTreeUtil() {
    }

    public static Set<Long> collectSubtreeIds(Long rootId, List<Comment> comments) {
        Map<Long, List<Comment>> childrenByParentId = comments.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> deleteIds = new LinkedHashSet<>();
        queue.add(rootId);
        deleteIds.add(rootId);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            for (Comment child : childrenByParentId.getOrDefault(parentId, Collections.emptyList())) {
                if (deleteIds.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }
        return deleteIds;
    }
}
