package ru.ifmo.rain.kurilenko.studentdb;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class StudentDB implements StudentQuery {
    private List<String> map(final List<Student> students, Function<? super Student, ? extends String> m) {
        return students.stream()
                .map(m)
                .collect(Collectors.toList());
    }

    public List<String> getFirstNames(final List<Student> students) {
        return map(students, Student::getFirstName);
    }

    public List<String> getLastNames(final List<Student> students) {
        return map(students, Student::getLastName);
    }

    public List<String> getGroups(final List<Student> students) {
        return map(students, Student::getGroup);
    }

    public List<String> getFullNames(final List<Student> students) {
        return map(students, s -> s.getFirstName() + " " + s.getLastName());
    }

    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .distinct()
                .sorted()
                .collect(Collectors.toSet());
    }

    public String getMinStudentFirstName(final List<Student> students) {
        return sortStudentsById(students)
                .get(0)
                .getFirstName();
    }

    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream()
                .sorted(Comparator.comparingInt(Student::getId))
                .collect(Collectors.toList());
    }

    private Comparator<Student> compareByName() {
        return Comparator.comparing(Student::getLastName)
                .thenComparing(Student::getFirstName)
                .thenComparingInt(Student::getId);
    }

    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream()
                .sorted(compareByName())
                .collect(Collectors.toList());
    }

    private List<Student> findStudents(Collection<Student> students, Predicate<? super Student> p) {
        return students.stream()
                .filter(p)
                .sorted(compareByName())
                .collect(Collectors.toList());
    }

    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudents(students, p -> p.getFirstName().equals(name));
    }

    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudents(students, p -> p.getLastName().equals(name));
    }

    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudents(students, p -> p.getGroup().equals(group));
    }

    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final String group) { //wtf is that
        return students.stream()
                .filter(p -> p.getGroup().equals(group))
                .collect(Collectors.groupingBy(Student::getLastName,
                        Collectors.collectingAndThen(Collectors.minBy(Comparator.comparing(Student::getFirstName)),
                                o -> o.get().getFirstName())));
    }
}

