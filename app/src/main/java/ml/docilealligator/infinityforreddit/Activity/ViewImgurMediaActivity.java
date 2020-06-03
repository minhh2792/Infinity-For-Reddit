package ml.docilealligator.infinityforreddit.Activity;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.docilealligator.infinityforreddit.API.ImgurAPI;
import ml.docilealligator.infinityforreddit.ContentFontStyle;
import ml.docilealligator.infinityforreddit.FontStyle;
import ml.docilealligator.infinityforreddit.Fragment.ViewImgurGifFragment;
import ml.docilealligator.infinityforreddit.Fragment.ViewImgurImageFragment;
import ml.docilealligator.infinityforreddit.Fragment.ViewImgurVideoFragment;
import ml.docilealligator.infinityforreddit.ImgurMedia;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.TitleFontStyle;
import ml.docilealligator.infinityforreddit.Utils.APIUtils;
import ml.docilealligator.infinityforreddit.Utils.JSONUtils;
import ml.docilealligator.infinityforreddit.Utils.SharedPreferencesUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ViewImgurMediaActivity extends AppCompatActivity {

    public static final String EXTRA_IMGUR_TYPE = "EIT";
    public static final String EXTRA_IMGUR_ID = "EII";
    public static final int IMGUR_TYPE_GALLERY = 0;
    public static final int IMGUR_TYPE_ALBUM = 1;
    public static final int IMGUR_TYPE_IMAGE = 2;
    private static final String IMGUR_IMAGES_STATE = "IIS";

    @BindView(R.id.progress_bar_view_imgur_media_activity)
    ProgressBar progressBar;
    @BindView(R.id.view_pager_view_imgur_media_activity)
    ViewPager viewPager;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ArrayList<ImgurMedia> images;
    @Inject
    @Named("imgur")
    Retrofit imgurRetrofit;
    @Inject
    @Named("default")
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((Infinity) getApplication()).getAppComponent().inject(this);

        getTheme().applyStyle(R.style.Theme_Normal, true);

        getTheme().applyStyle(FontStyle.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.FONT_SIZE_KEY, FontStyle.Normal.name())).getResId(), true);

        getTheme().applyStyle(TitleFontStyle.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.TITLE_FONT_SIZE_KEY, TitleFontStyle.Normal.name())).getResId(), true);

        getTheme().applyStyle(ContentFontStyle.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.CONTENT_FONT_SIZE_KEY, ContentFontStyle.Normal.name())).getResId(), true);

        setContentView(R.layout.activity_view_imgur_media);

        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
        actionBar.setHomeAsUpIndicator(upArrow);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparentActionBarAndExoPlayerControllerColor)));

        setTitle(" ");

        String imgurId = getIntent().getStringExtra(EXTRA_IMGUR_ID);
        if (imgurId == null) {
            finish();
            return;
        }

        if (savedInstanceState != null) {
            images = savedInstanceState.getParcelableArrayList(IMGUR_IMAGES_STATE);
        }

        if (images == null) {
            switch (getIntent().getIntExtra(EXTRA_IMGUR_TYPE, IMGUR_TYPE_IMAGE)) {
                case IMGUR_TYPE_GALLERY:
                    imgurRetrofit.create(ImgurAPI.class).getGalleryImages(APIUtils.IMGUR_CLIENT_ID, imgurId)
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                    if (response.isSuccessful()) {
                                        new ParseImgurImagesAsyncTask(response.body(), new ParseImgurImagesAsyncTask.ParseImgurImagesAsyncTaskListener() {
                                            @Override
                                            public void success(ArrayList<ImgurMedia> images) {
                                                ViewImgurMediaActivity.this.images = images;
                                                progressBar.setVisibility(View.GONE);
                                                setupViewPager();
                                            }

                                            @Override
                                            public void failed() {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(ViewImgurMediaActivity.this, R.string.error_fetching_imgur_media, Toast.LENGTH_SHORT).show();
                                            }
                                        }).execute();
                                    } else {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(ViewImgurMediaActivity.this, R.string.error_fetching_imgur_media, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ViewImgurMediaActivity.this, R.string.error_fetching_imgur_media, Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
                case IMGUR_TYPE_ALBUM:
                    break;
                case IMGUR_TYPE_IMAGE:
                    break;
            }
        } else {
            progressBar.setVisibility(View.GONE);
            setupViewPager();
        }
    }

    private void setupViewPager() {
        setToolbarTitle(0);
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (images.get(position).getType()) {
                    case ImgurMedia.TYPE_GIF:
                        setTitle("GIF " + (position + 1) + "/" + images.size());
                        break;
                    case ImgurMedia.TYPE_VIDEO:
                        setTitle("Video "+ (position + 1) + "/" + images.size());
                        break;
                    default:
                        setTitle("Image " + (position + 1) + "/" + images.size());

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setAdapter(sectionsPagerAdapter);
    }

    private void setToolbarTitle(int position) {
        switch (images.get(position).getType()) {
            case ImgurMedia.TYPE_GIF:
                setTitle("GIF " + (position + 1) + "/" + images.size());
                break;
            case ImgurMedia.TYPE_VIDEO:
                setTitle("Video "+ (position + 1) + "/" + images.size());
                break;
            default:
                setTitle("Image " + (position + 1) + "/" + images.size());

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(IMGUR_IMAGES_STATE, images);
    }

    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        SectionsPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            ImgurMedia imgurMedia = images.get(position);
            if (imgurMedia.getType() == ImgurMedia.TYPE_VIDEO) {
                ViewImgurVideoFragment fragment = new ViewImgurVideoFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable(ViewImgurVideoFragment.EXTRA_IMGUR_VIDEO, imgurMedia);
                fragment.setArguments(bundle);
                return fragment;
            } else if (imgurMedia.getType() == ImgurMedia.TYPE_GIF) {
                ViewImgurGifFragment fragment = new ViewImgurGifFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable(ViewImgurGifFragment.EXTRA_IMGUR_GIF, imgurMedia);
                fragment.setArguments(bundle);
                return fragment;
            } else {
                ViewImgurImageFragment fragment = new ViewImgurImageFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable(ViewImgurImageFragment.EXTRA_IMGUR_IMAGES, imgurMedia);
                fragment.setArguments(bundle);
                return fragment;
            }
        }

        @Override
        public int getCount() {
            return images.size();
        }
    }

    private static class ParseImgurImagesAsyncTask extends AsyncTask<Void, Void, Void> {

        private String response;
        private ArrayList<ImgurMedia> images;
        private boolean parseFailed = false;
        private ParseImgurImagesAsyncTaskListener parseImgurImagesAsyncTaskListener;

        interface ParseImgurImagesAsyncTaskListener {
            void success(ArrayList<ImgurMedia> images);
            void failed();
        }

        ParseImgurImagesAsyncTask(String response, ParseImgurImagesAsyncTaskListener parseImgurImagesAsyncTaskListener) {
            this.response = response;
            this.parseImgurImagesAsyncTaskListener = parseImgurImagesAsyncTaskListener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONArray jsonArray = new JSONObject(response).getJSONObject(JSONUtils.DATA_KEY).getJSONArray(JSONUtils.IMAGES_KEY);
                images = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject image = jsonArray.getJSONObject(i);
                    images.add(new ImgurMedia(image.getString(JSONUtils.ID_KEY),
                            image.getString(JSONUtils.TITLE_KEY), image.getString(JSONUtils.DESCRIPTION_KEY),
                            image.getString(JSONUtils.TYPE_KEY), image.getString(JSONUtils.LINK_KEY)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                parseFailed = true;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (parseFailed) {
                parseImgurImagesAsyncTaskListener.failed();
            } else {
                parseImgurImagesAsyncTaskListener.success(images);
            }
        }
    }
}