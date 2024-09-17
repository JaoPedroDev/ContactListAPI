package io.jaopedrodev.contactapi.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.jaopedrodev.contactapi.domain.Contact;
import io.jaopedrodev.contactapi.repo.ContactRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ContactService {
    private final ContactRepo contactRepo;

    public Page<Contact> getAllContacts(int page, int size) {
        log.info("Fetching all users with page: {} and size: {}", page, size);
        return contactRepo.findAll(PageRequest.of(page, size, Sort.by("name")));
    }
    
    public Contact getContactById(String id) {
        log.info("Fetching user with id: {}", id);
        return contactRepo.findById(id).orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public Contact createContact (Contact contact) {
        log.info("Saving new user with id: {}", contact.getId());
        return contactRepo.save(contact);
    }

    public Contact updateContact(Contact contact) {
        log.info("Updating user with id: {}", contact.getId());
        return contactRepo.save(contact);
    }

    public void deleteContact(Contact contact) {
        String photoUrl = contact.getPhotoUrl();
        if (photoUrl != null) {
            deletePhoto(photoUrl);
        }
        log.info("Deleting user with id: {}", contact.getId());
        contactRepo.delete(contact);
    }

    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Uploading photo for user id: {}", id);
        Contact contact = getContactById(id);
        if (contact.getPhotoUrl() != null) {
            deletePhoto(contact.getPhotoUrl());
        }
        String photoUrl = photoFunction.apply(id, file);
        contact.setPhotoUrl(photoUrl);
        contactRepo.save(contact);
        return photoUrl;
    }

    private void deletePhoto (String photoUrl) {
        try {
            int index = photoUrl.lastIndexOf("/");
            String fileName = photoUrl.substring(index + 1);
            Path fileStorageLocation = Paths.get("./uploads/" + fileName).toAbsolutePath().normalize();
            log.info(fileStorageLocation.toString());
            Files.deleteIfExists(fileStorageLocation);
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete image");
        }
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
        .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get("./uploads").toAbsolutePath().normalize();
            if(!Files.exists(fileStorageLocation)) {Files.createDirectories(fileStorageLocation);}
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/contacts/image/" + filename).toUriString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to save image");
        }
    };
}
