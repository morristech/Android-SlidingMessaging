package com.klinker.android.messaging_card.batch_delete;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.klinker.android.messaging_card.MainActivity;
import com.klinker.android.messaging_donate.R;

public class BatchDeleteArrayAdapter extends ArrayAdapter<String> {
	  private final Activity context;
	  private final ArrayList<String> numbers;
	  private SharedPreferences sharedPrefs;
	  private Resources resources;
	  public static ArrayList<Integer> itemsToDelete = new ArrayList<Integer>();
	  public static View defaultBackground;
	  
	  static class ViewHolder {
		    public TextView name;
		    public TextView number;
		    public TextView other;
		    public TextView other2;
		    public TextView other3;
		    public QuickContactBadge image;
		    public ImageButton callButton;
		    public View background;
		    public View divider1;
		    public View divider2;
		  }

	  public BatchDeleteArrayAdapter(Activity context, ArrayList<String> numbers) {
	    super(context, R.layout.contact_card, numbers.size());
	    this.context = context;
	    this.numbers = numbers;
	    this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	    this.resources = context.getResources();
	  }
	  
	  @Override
	  public int getCount()
	  {
		return numbers.size();
	  }

	  @SuppressWarnings("deprecation")
	@SuppressLint("SimpleDateFormat")
	  @Override
	  public View getView(final int position, View convertView, ViewGroup parent) {
		  View contactView = convertView;
		  
		  if (contactView == null)
		  {
			  LayoutInflater inflater = (LayoutInflater) context
				        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
			  if (sharedPrefs.getString("card_theme", "Light").equals("Light"))
			  {
				  contactView = inflater.inflate(R.layout.contact_card, parent, false);
			  } else if (sharedPrefs.getString("card_theme", "Light").equals("Dark"))
			  {
				  contactView = inflater.inflate(R.layout.contact_card_dark, parent, false);
			  } else if (sharedPrefs.getString("card_theme", "Light").equals("Pitch Black"))
			  {
				  contactView = inflater.inflate(R.layout.contact_card_dark, parent, false);
			  }
			  
			  ViewHolder viewHolder = new ViewHolder();
			  viewHolder.name = (TextView) contactView.findViewById(R.id.contactName);
			  viewHolder.number = (TextView) contactView.findViewById(R.id.contactNumberType);
			  viewHolder.image = (QuickContactBadge) contactView.findViewById(R.id.contactPicture);
			  viewHolder.callButton = (ImageButton) contactView.findViewById(R.id.callButton);
			  viewHolder.background = (View) contactView.findViewById(R.id.view1);
			  viewHolder.divider1 = (View) contactView.findViewById(R.id.contactLine);
			  viewHolder.divider2 = (View) contactView.findViewById(R.id.contactLine2); 
			  viewHolder.other = (TextView) contactView.findViewById(R.id.contactNumber);
			  viewHolder.other2 = (TextView) contactView.findViewById(R.id.deleteText);
			  viewHolder.other3 = (TextView) contactView.findViewById(R.id.allText);
			  
			  if (sharedPrefs.getBoolean("custom_font", false))
			  {
				  viewHolder.name.setTypeface(MainActivity.font);
				  viewHolder.number.setTypeface(MainActivity.font);
			  }
			  
			  viewHolder.name.setTextSize((float)Integer.parseInt(sharedPrefs.getString("text_size2", 14 + "")));
			  viewHolder.number.setTextSize((float)(Integer.parseInt(sharedPrefs.getString("text_size2", 14 + "")) - 2));
			  
			  viewHolder.other.setVisibility(View.INVISIBLE);
			  viewHolder.other2.setVisibility(View.INVISIBLE);
			  viewHolder.other3.setVisibility(View.INVISIBLE);
			  
			  viewHolder.callButton.setVisibility(View.GONE);
			  viewHolder.divider2.setVisibility(View.GONE);
			  
			  defaultBackground = viewHolder.background;
			  
			  contactView.setTag(viewHolder);
		  }
		  
		  final ViewHolder holder = (ViewHolder) contactView.getTag();

          final String number = com.klinker.android.messaging_sliding.MainActivity.findContactNumber(numbers.get(position), context);
		  
		  holder.number.setText(number);
		  holder.image.assignContactFromPhone(number, true);
		  
		  if (itemsToDelete.contains(position))
		  {
			  holder.background.setBackgroundColor(context.getResources().getColor(R.color.holo_blue));
		  } else
		  {
			  if (sharedPrefs.getString("card_theme", "Light").equals("Light"))
				{
					holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.card_background));
				} else if (sharedPrefs.getString("card_theme", "Light").equals("Dark"))
				{
					holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.card_background_dark));
				} else if (sharedPrefs.getString("card_theme", "Light").equals("Pitch Black"))
				{
					holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.card_background_black));
				}
		  }
		  
		  new Thread(new Runnable() {

			@Override
			public void run() {
				final String name = loadGroupContacts(number, context);
				final Bitmap picture = getFacebookPhoto(number);
				
				((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content).post(new Runnable() {
            		
					@Override
					public void run() {
						holder.name.setText(name);
						holder.image.setImageBitmap(picture);
						
						if (holder.name.getText().toString().replace("-", "").equals(holder.number.getText().toString()))
						{
							holder.number.setVisibility(View.GONE);
						} else
						{
							holder.number.setVisibility(View.VISIBLE);
						}
					}
			    	
			    });
				
			}
			  
		  }).start();
		  
		  contactView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				boolean flag = false;
				int pos = 0;
				
				for (int i = 0; i < itemsToDelete.size(); i++)
				{
					if (itemsToDelete.get(i) == position)
					{
						flag = true;
						pos = i;
						break;
					}
				}
				
				if (!flag)
				{
					itemsToDelete.add(position);
					holder.background.setBackgroundColor(context.getResources().getColor(R.color.holo_blue));
				} else
				{
					itemsToDelete.remove(pos);
					
					if (sharedPrefs.getString("card_theme", "Light").equals("Light"))
					{
						holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.card_background));
					} else if (sharedPrefs.getString("card_theme", "Light").equals("Dark"))
					{
						holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.card_background_dark));
					} else if (sharedPrefs.getString("card_theme", "Light").equals("Pitch Black"))
					{
						holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.card_background_black));
					}
				}
				
			}
			  
		  });

		  return contactView;
	  }
	  
	  public InputStream openDisplayPhoto(long contactId) {
		  Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
		     Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
		     Cursor cursor = context.getContentResolver().query(photoUri,
		          new String[] {Contacts.Photo.PHOTO}, null, null, null);
		     if (cursor == null) {
		         return null;
		     }
		     try {
		         if (cursor.moveToFirst()) {
		             byte[] data = cursor.getBlob(0);
		             if (data != null) {
		                 return new ByteArrayInputStream(data);
		             }
		         }
		     } finally {
		         cursor.close();
		     }
		     return null;
		 }
	  	
	  	public Bitmap getFacebookPhoto(String phoneNumber) {
		    Uri phoneUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		    Uri photoUri = null;
		    ContentResolver cr = context.getContentResolver();
		    Cursor contact = cr.query(phoneUri,
		            new String[] { ContactsContract.Contacts._ID }, null, null, null);

		    try
		    {
			    if (contact.moveToFirst()) {
			        long userId = contact.getLong(contact.getColumnIndex(ContactsContract.Contacts._ID));
			        photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, userId);
		
			    }
			    else {
			        Bitmap defaultPhoto = BitmapFactory.decodeResource(resources, R.drawable.card_person);
			        return defaultPhoto;
			    }
			    if (photoUri != null) {
			        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
			                cr, photoUri);
			        if (input != null) {
			            return BitmapFactory.decodeStream(input);
			        }
			    } else {
			        Bitmap defaultPhoto = BitmapFactory.decodeResource(resources, R.drawable.card_person);
			        return defaultPhoto;
			    }
			    Bitmap defaultPhoto = BitmapFactory.decodeResource(resources, R.drawable.card_person);
			    return defaultPhoto;
		    } catch (Exception e)
		    {
		    	return BitmapFactory.decodeResource(resources, R.drawable.card_person);
		    }
		}
	  
	  public Bitmap drawableToBitmap (Drawable drawable) {
		    if (drawable instanceof BitmapDrawable) {
		        return ((BitmapDrawable)drawable).getBitmap();
		    }

		    try
		    {
			    int width = drawable.getIntrinsicWidth();
			    width = width > 0 ? width : 1;
			    int height = drawable.getIntrinsicHeight();
			    height = height > 0 ? height : 1;
		
			    Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			    Canvas canvas = new Canvas(bitmap); 
			    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			    drawable.draw(canvas);
			    return bitmap;
		    } catch (Exception e)
		    {
		    	return BitmapFactory.decodeResource(resources, R.drawable.ic_contact_picture);
		    }
		}
	  
	  public static String loadGroupContacts(String numbers, Context context)
		{
			String names = "";
			String[] number = numbers.split(" ");
			
			for (int i = 0; i < number.length; i++)
			{				
				try
				{
					String origin = number[i];
					
					Uri phoneUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(origin));
					Cursor phonesCursor = context.getContentResolver().query(phoneUri, new String[] {ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.RawContacts._ID}, null, null, ContactsContract.Contacts.DISPLAY_NAME + " desc limit 1");
		
					if(phonesCursor != null && phonesCursor.moveToFirst()) {
						names += ", " + phonesCursor.getString(0);
					} else
					{
						try
						{
							Locale sCachedLocale = Locale.getDefault();
							int sFormatType = PhoneNumberUtils.getFormatTypeForLocale(sCachedLocale);
							Editable editable = new SpannableStringBuilder(number[i]);
							PhoneNumberUtils.formatNumber(editable, sFormatType);
							names += ", " + editable.toString();
						} catch (Exception e)
						{
							names += ", " + number;
						}
					}
					
					phonesCursor.close();
				} catch (IllegalArgumentException e)
				{
					try
					{
						Locale sCachedLocale = Locale.getDefault();
						int sFormatType = PhoneNumberUtils.getFormatTypeForLocale(sCachedLocale);
						Editable editable = new SpannableStringBuilder(number[i]);
						PhoneNumberUtils.formatNumber(editable, sFormatType);
						names += ", " + editable.toString();
					} catch (Exception f)
					{
						names += ", " + number;
					}
				}
			}
			
			return names.substring(2);
		}

}
